package dev.webfx.platform.substitution.var.spi.impl.prompt;

import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.impl.ThreadLocalConfigContext;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.substitution.var.spi.impl.VariablesResolverBase;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * Resolves a configuration variable by asking the developer to type its value when the application starts, so that a
 * secret such as a database password doesn't have to be stored on disk - neither in a conf file nor in the environment
 * variables of an IDE run configuration (which the IDE also stores in plain text).
 *
 * <p>It is opt-in: it only resolves the variable names written with the {@code prompt:} prefix, for example:
 * <pre>
 * password = ${{ DB_PASSWORD | prompt:DB_PASSWORD }}
 * password = ${{ DB_PASSWORD | prompt:DB_PASSWORD(username, host, databaseName) }}
 * </pre>
 * The alternatives are tried from left to right, so a system property or environment variable still wins when it is
 * set (servers, CI), and the developer is asked only when nothing else resolved the value. The optional keys in
 * parentheses are sibling keys of the config being read, whose values are displayed in the prompt so the developer can
 * tell which target the value is asked for. List only non-secret keys there, as their values are displayed in clear.
 *
 * <p>The typed value (or the refusal to type it) is kept in memory for the lifetime of the JVM, because the
 * substitution is re-evaluated each time the config key is read, and the developer should be asked only once.
 *
 * <p>A password dialog is shown when a desktop is available (ex: a run from the IDE, whose console would echo the typed
 * characters), otherwise the value is read from the terminal with echo disabled. Where nobody can answer (ex: a
 * container), the variable is simply not resolved - this never blocks. Note that the startup is paused while the
 * developer types, so an event loop resolving the config at that time (ex: Vert.x) may report itself as blocked.
 *
 * @author Bruno Salmon
 */
public final class PromptVariablesResolver extends VariablesResolverBase {

    private static final String PREFIX = "prompt:";

    // Guarded by ANSWERS. An empty Optional records a refusal (cancelled or impossible prompt), so it's not asked again.
    private static final Map<String, Optional<String>> ANSWERS = new HashMap<>();
    // Variables being prompted, to break the loop if their context keys lead back to them
    private static final Set<String> PROMPTING = new HashSet<>();

    @Override
    public Optional<String> resolveVariable(String variableName) {
        if (!variableName.startsWith(PREFIX))
            return Optional.empty();
        String spec = variableName.substring(PREFIX.length()).trim();
        int openingParenthesis = spec.indexOf('(');
        String name = (openingParenthesis < 0 ? spec : spec.substring(0, openingParenthesis)).trim();
        List<String> contextKeys = openingParenthesis < 0 || !spec.endsWith(")") ? List.of() :
            Arrays.stream(spec.substring(openingParenthesis + 1, spec.length() - 1).split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .toList();
        // Synchronized, so that concurrent reads wait for the single prompt rather than asking again
        synchronized (ANSWERS) {
            Optional<String> answer = ANSWERS.get(name);
            if (answer == null) {
                if (!PROMPTING.add(name))
                    return Optional.empty();
                try {
                    answer = Optional.ofNullable(prompt(name, readContext(contextKeys)));
                } finally {
                    PROMPTING.remove(name);
                }
                ANSWERS.put(name, answer);
            }
            return passVariableSearchResult(name, answer.orElse(null), "prompt");
        }
    }

    private static List<String> readContext(List<String> contextKeys) {
        List<String> context = new ArrayList<>();
        Config config = ThreadLocalConfigContext.getThreadLocalConfig();
        if (config != null) {
            for (String key : contextKeys) {
                String value = config.getString(key);
                if (value != null)
                    context.add(key + " = " + value);
            }
        }
        return context;
    }

    private static String prompt(String name, List<String> context) {
        try {
            if (!GraphicsEnvironment.isHeadless())
                return promptInDialog(name, context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception | LinkageError | AWTError e) { // Ex: no display server, or a JDK without the native AWT library
            Console.log("⚠️ Couldn't open the dialog to type ‹ " + name + " › (" + e + ")");
        }
        java.io.Console terminal = System.console();
        if (isTerminal(terminal)) {
            context.forEach(line -> terminal.printf("%s%n", line));
            return toStringAndWipe(terminal.readPassword("%s: ", name));
        }
        Console.log("⚠️ ‹ " + name + " › can't be prompted, as neither a desktop nor a terminal is available");
        return null;
    }

    private static String promptInDialog(String name, List<String> context) throws InterruptedException, InvocationTargetException {
        Console.log("🔑 Waiting for ‹ " + name + " › to be typed in the dialog (the startup is paused until then)");
        String[] typedValue = { null };
        Runnable dialogRunnable = () -> {
            JPasswordField passwordField = new JPasswordField(24);
            Box message = Box.createVerticalBox();
            message.add(newPlainLabel("Please type the value of " + name));
            context.forEach(line -> message.add(newPlainLabel(line)));
            message.add(Box.createVerticalStrut(8));
            message.add(passwordField);
            JOptionPane optionPane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
                @Override
                public void selectInitialValue() { // Focusing the password field rather than the OK button
                    passwordField.requestFocusInWindow();
                }
            };
            passwordField.addActionListener(e -> optionPane.setValue(JOptionPane.OK_OPTION)); // Enter = OK
            JDialog dialog = optionPane.createDialog(null, name);
            // The application has no window of its own, so the dialog would otherwise open behind the IDE
            dialog.setAlwaysOnTop(true);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND))
                Desktop.getDesktop().requestForeground(true);
            dialog.setVisible(true); // Modal, so returns once the dialog is closed
            char[] password = passwordField.getPassword();
            if (Objects.equals(optionPane.getValue(), JOptionPane.OK_OPTION))
                typedValue[0] = toStringAndWipe(password);
            else
                Arrays.fill(password, '\0');
            dialog.dispose();
        };
        if (SwingUtilities.isEventDispatchThread())
            dialogRunnable.run();
        else
            SwingUtilities.invokeAndWait(dialogRunnable);
        return typedValue[0];
    }

    private static JLabel newPlainLabel(String text) {
        JLabel label = new JLabel();
        // Displaying config values as text, never as HTML. Set before the text, as the label UI reads it on text changes.
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setText(text);
        return label;
    }

    private static boolean isTerminal(java.io.Console console) {
        if (console == null)
            return false;
        try { // Since JDK 22, System.console() is not null even when not attached to a terminal, which isTerminal() tells
            return (Boolean) java.io.Console.class.getMethod("isTerminal").invoke(console);
        } catch (NoSuchMethodException e) { // Before JDK 22, a console is always attached to a terminal
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static String toStringAndWipe(char[] chars) {
        if (chars == null) // Ex: end of stream on the terminal
            return null;
        String value = new String(chars);
        Arrays.fill(chars, '\0');
        return value;
    }
}
