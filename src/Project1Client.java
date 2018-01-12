import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Project1Client {

    private Socket client;

    public Project1Client(InetAddress remoteAddress, int port) throws IOException
    {
        client = new Socket(remoteAddress, port);

    }

    // Used for testing client->server communication.
    public void chatDemo()
    {
        System.out.println("Whatever you type will be sent to the server. Type 'exit' or 'quit' to disconnect.");
        Scanner read = new Scanner(System.in);
        while (client.isConnected())
        {
            System.out.print(">> ");
            String line = read.nextLine();
            try {
                client.getOutputStream().write(StandardCharsets.UTF_8.encode(line).array());
            } catch (IOException ex) {
                System.out.format("\nError! %s\n", ex.getMessage());
                return;
            }
        }
        System.out.println("Disconnected from server.");
    }

    public void interact()
    {
        Scanner read = new Scanner(System.in);
        showMenu();

        while (true) {
            System.out.print("> ");
            String uin = read.nextLine().trim();
            if (equalsAnyIgnoreCase(uin, "quit", "stop", "exit"))
                return;

            Operation op = null;
            // Try parsing the choice as a number.
            Integer choice = tryParseInteger(uin.replaceAll("[\\p{Punct}]", ""));

            if (choice == null)
            {
                // Try parsing the choice as a word.
                List<Operation> ops = _operations.stream().filter(o -> o.matches(uin)).collect(Collectors.toCollection(ArrayList<Operation>::new));
                if (ops.size() == 0)
                {
                    // Search returned no results.
                    System.out.println("Invalid choice.");
                    showMenu();
                    continue;
                }
                if (ops.size() > 1)
                {
                    // There was some ambiguity when attempting to map the input to the valid operations.
                    // This should never happen in deployment, since nicknames should be mutually exclusive.

                    System.out.format("Did you mean %s?", orList(ops.stream().map(o -> String.valueOf(_operations.indexOf(o) + 1)).toArray(String[]::new)));
                    showMenu();
                }
                // Search returned exactly one result.
                op = ops.get(0);
            } else {
                if (choice == 0)
                    return; // 0 means quit.

                if (choice < 1 || choice > _operations.size()) {
                    System.out.println("Invalid choice.");
                    showMenu();
                    continue;
                }
                // Choice was in range.
                op = _operations.get(choice - 1);
            }

                System.out.format("OPERATION SELECTED: %s\n", op.getDescription());

            // Send the command to the server.
           /*
            try {
                writeByte(op.code);

                // todo: get response and display it
            } catch (IOException ex) {
                System.out.format("Error communicating with server: %s\n", ex.getMessage());
            }
            */

        }

    }

    private static boolean equalsAnyIgnoreCase(String comparand, String... choices)
    {
        for (String c : choices)
        {
            if (comparand.equalsIgnoreCase(c))
                return true;
        }
        return false;
    }

    // Formats a list of strings as a pretty string with commas and "or".
    private static String orList(String... items)
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < items.length - 1; ++i)
        {
            sb.append(items[i]);
            sb.append(", ");
        }
        sb.append("or ");
        sb.append(items[items.length - 1]);
        return sb.toString();
    }

    // Writes a single byte to the socket.
    private void writeByte(byte b) throws IOException
    {
        client.getOutputStream().write(b);
    }

    private static Integer tryParseInteger(String s)
    {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }


    private static List<Operation> _operations;

    static {
        _operations = new ArrayList<>(6);
        // Server and client must agree on the byte ("code" parameter).
        _operations.add(new Operation("Get host date & time",  (byte)11,  "date", "time"));
        _operations.add(new Operation("Get host uptime",  (byte)22,  "uptime"));
        _operations.add(new Operation("Get host memory usage",  (byte)33,  "memory", "mem"));
        _operations.add(new Operation("Get host netstat output",  (byte)44,  "netstat"));
        _operations.add(new Operation("Get host current users",  (byte)55,  "users", "who"));
        _operations.add(new Operation("Get host running processes",  (byte)66,  "process", "processes", "ps"));
    }

    static class Operation
    {
        private String descr;
        private String[] nicks;
        private byte code;

        /*
         * @param description   A description of the operation that will be shown to the user.
         * @param code          The byte that uniquely identifies this operation to the remote host.
         * @param nicknames     A collection of nicknames that this operation should match. Nicknames must be mutually exclusive among all operations.
         */
        public Operation(String description, byte code, String... nicknames)
        {
            descr = description;
            this.code = code;
            nicks = nicknames;
        }

        // Returns true iff the given text matches any of this Operation's nicknames.
        public boolean matches(String text)
        {
            return equalsAnyIgnoreCase(text, nicks);
        }

        public String getDescription()
        {
            return descr;
        }

        public byte getCode()
        {
            return code;
        }
    }

    private void showMenu()
    {
        System.out.format("---There are %d supported operations-------------\n", _operations.size());
        for (int i=0; i < _operations.size(); ++i)
        {
            System.out.format(" %d. %s\n", i + 1, _operations.get(i).getDescription());
        }
        System.out.println("Enter 0 to quit.");
        System.out.println();
    }

}
