package dialogues;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dialogues {
    public static boolean doubleCheck(String value) {
        return JOptionPane.showConfirmDialog(null, "Proceed with '" + value + "'?") == JOptionPane.OK_OPTION;
    }

    public static int askMaxWorldSize(String[] ar) throws NumberFormatException {
        int max = -1;
        if (ar.length > 1) {
            max = Integer.parseInt(ar[1]);
        }

        while (max < 1) {
            String maxStr = JOptionPane.showInputDialog("max-world-size? (same as of server.properties)");
            if (maxStr == null)
                return max;

            if (!doubleCheck(maxStr))
                continue;

            try {
                max = Integer.parseInt(maxStr);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }

        return max;
    }

    private static final Pattern namePattern = Pattern.compile("^[a-zA-Z]+[0-9]*$");

    public static String askWorldName(String[] ar) throws IllegalArgumentException {
        String name = null;
        if (ar.length > 0) {
            Matcher matcher = namePattern.matcher(ar[0]);
            if (!matcher.matches())
                throw new IllegalArgumentException(ar[0] + " is not a valid world name.");
        }

        while (true) {
            name = JOptionPane.showInputDialog("world name?");
            if (name == null)
                return null;

            Matcher matcher = namePattern.matcher(name);
            if (!matcher.matches()) {
                JOptionPane.showMessageDialog(null, name + " is not a valid name to be used.\n" +
                        "Use only lower or upper English letters as name.\n" +
                        "Also, digits can be placed after English letters.\n" +
                        "(world123 is okay, but 123world is not.)");
                continue;
            }

            if (!doubleCheck(name)) {
                continue;
            }

            return name;
        }
    }
}
