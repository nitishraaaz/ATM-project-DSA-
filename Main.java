import java.io.*;
import java.util.*;

public class Main {

    static class Account {
        String accNo;
        String name;
        String pin;
        int balance;
        boolean blocked;

        Account(String accNo, String name, String pin, int balance, boolean blocked) {
            this.accNo = accNo;
            this.name = name;
            this.pin = pin;
            this.balance = balance;
            this.blocked = blocked;
        }

        @Override
        public String toString() {
            return accNo + "," + name + "," + pin + "," + balance + "," + blocked;
        }
    }

    static final String FILE_NAME = "accounts.txt";
    static final int[] DENOMINATIONS = {500, 200, 100};
    static Account[] accounts = new Account[50];
    static int accountCount = 0;

    static Queue<String> customerQueue = new LinkedList<>();
    static Stack<String> transactionStack = new Stack<>();
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        loadAccounts();
        customerQueue.add("ACC101");

        while (true) {
            System.out.println("\n=== ATM QUEUE MENU ===");
            System.out.println("Waiting in queue: " + customerQueue);
            System.out.println("1. Serve next customer");
            System.out.println("2. Add customer to queue");
            System.out.println("3. Create new account");
            System.out.println("4. Show all accounts");
            System.out.println("5. Exit & Save");
            System.out.print("Enter choice: ");

            int choice = readInt();
            switch (choice) {
                case 1:
                    serveNextCustomer();
                    break;
                case 2:
                    System.out.print("Enter Account Number to join queue: ");
                    String accNo = sc.nextLine().trim();
                    if (findAccount(accNo) == null) {
                        System.out.println("Account " + accNo + " does not exist. Create it first.");
                    } else {
                        customerQueue.add(accNo);
                        System.out.println(accNo + " added to queue.");
                    }
                    break;
                case 3:
                    createNewAccount();
                    break;
                case 4:
                    showAllAccounts();
                    break;
                case 5:
                    saveAccounts();
                    System.out.println("Data saved. Exiting ATM Simulator.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    static void showAllAccounts() {
        if (accountCount == 0) {
            System.out.println("No accounts found.");
            return;
        }

        System.out.println("\n--- REGISTERED ACCOUNTS ---");
        System.out.printf("%-10s %-15s %-10s %-10s%n", "Acc No", "Name", "Balance", "Status");
        System.out.println("-----------------------------------------------");
        for (int i = 0; i < accountCount; i++) {
            Account acc = accounts[i];
            String status = acc.blocked ? "BLOCKED" : "ACTIVE";
            System.out.printf("%-10s %-15s %-10d %-10s%n", acc.accNo, acc.name, acc.balance, status);
        }
    }

    static void addAccountToArray(Account acc) {
        if (accountCount == accounts.length) {
            accounts = Arrays.copyOf(accounts, accounts.length * 2);
        }
        accounts[accountCount++] = acc;
    }

    static void createNewAccount() {
        System.out.print("Enter new Account Number (e.g., Ki745): ");
        String accNo = sc.nextLine().trim();
        if (accNo.isEmpty() || findAccount(accNo) != null) {
            System.out.println("Invalid or duplicate Account Number.");
            return;
        }

        System.out.print("Enter Customer Name: ");
        String name = sc.nextLine().trim();

        System.out.print("Set 4-digit PIN: ");
        String pin = sc.nextLine().trim();

        System.out.print("Enter Initial Balance: ");
        int balance = readInt();
        if (balance < 0) {
            System.out.println("Balance cannot be negative.");
            return;
        }

        addAccountToArray(new Account(accNo, name, pin, balance, false));
        saveAccounts();
        System.out.println("Account " + accNo + " created and saved to " + FILE_NAME + ".");
    }

    static void loadAccounts() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            addAccountToArray(new Account("ACC101", "Riya", "1234", 5000, false));
            saveAccounts();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    try {
                        String accNo = parts[0].trim();
                        String name = parts[1].trim();
                        String pin = parts[2].trim();
                        int balance = Integer.parseInt(parts[3].trim());
                        boolean blocked = parts.length >= 5 && Boolean.parseBoolean(parts[4].trim());
                        addAccountToArray(new Account(accNo, name, pin, balance, blocked));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading accounts: " + e.getMessage());
        }
    }

    static void saveAccounts() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (int i = 0; i < accountCount; i++) {
                pw.println(accounts[i].toString());
            }
        } catch (IOException e) {
            System.out.println("Error saving accounts: " + e.getMessage());
        }
    }

    static Account findAccount(String accNo) {
        for (int i = 0; i < accountCount; i++) {
            if (accounts[i].accNo.equalsIgnoreCase(accNo)) {
                return accounts[i];
            }
        }
        return null;
    }

    static void serveNextCustomer() {
        if (customerQueue.isEmpty()) {
            System.out.println("No customers waiting in the queue.");
            return;
        }

        String accNo = customerQueue.poll();
        System.out.println("\nServing customer: " + accNo);

        Account acc = findAccount(accNo);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }

        if (acc.blocked) {
            System.out.println("Card is blocked due to multiple wrong PIN attempts.");
            return;
        }

        int attemptsLeft = 3;
        boolean authenticated = false;

        while (attemptsLeft > 0) {
            System.out.print("PIN: ");
            String enteredPin = sc.nextLine().trim();

            if (acc.pin.equals(enteredPin)) {
                authenticated = true;
                break;
            } else {
                attemptsLeft--;
                if (attemptsLeft > 0) {
                    System.out.println("Incorrect PIN. Attempts left: " + attemptsLeft);
                } else {
                    acc.blocked = true;
                    saveAccounts();
                    System.out.println("Incorrect PIN. Card blocked.");
                }
            }
        }

        if (authenticated) {
            transactionStack.clear();
            System.out.println("Login successful. Welcome, " + acc.name + ".");
            checkBalance(acc);
            customerSession(acc);
        }
    }

    static void customerSession(Account acc) {
        while (true) {
            System.out.println("\n--- ATM MENU (" + acc.name + ") ---");
            System.out.println("2. Check Balance");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. Mini Statement");
            System.out.println("6. Logout");
            System.out.print("Enter option: ");

            int option = readInt();
            switch (option) {
                case 2:
                    checkBalance(acc);
                    break;
                case 3:
                    deposit(acc);
                    break;
                case 4:
                    withdraw(acc);
                    break;
                case 5:
                    miniStatement();
                    break;
                case 6:
                    saveAccounts();
                    System.out.println("Logged out.");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    static void checkBalance(Account acc) {
        System.out.println("Balance: " + acc.balance);
    }

    static void deposit(Account acc) {
        System.out.print("Enter deposit amount: ");
        int amount = readInt();

        if (amount <= 0) {
            System.out.println("Deposit amount must be positive.");
            return;
        }

        acc.balance += amount;
        transactionStack.push("Deposit " + amount + " | Balance " + acc.balance);
        saveAccounts();
        System.out.println("Deposit successful. Balance: " + acc.balance);
    }

    static void withdraw(Account acc) {
        System.out.print("Enter withdrawal amount: ");
        int amount = readInt();

        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("Invalid amount. Withdrawal must be a positive multiple of 100.");
            return;
        }
        if (amount > acc.balance) {
            System.out.println("Insufficient balance.");
            return;
        }

        int remaining = amount;
        List<String> dispensedParts = new ArrayList<>();

        for (int note : DENOMINATIONS) {
            int count = remaining / note;
            if (count > 0) {
                dispensedParts.add(note + " x " + count);
                remaining %= note;
            }
        }

        acc.balance -= amount;
        transactionStack.push("Withdraw " + amount + " | Balance " + acc.balance);
        saveAccounts();

        System.out.println("Dispensed: " + String.join(", ", dispensedParts));
        System.out.println("Balance: " + acc.balance);
    }

    static void miniStatement() {
        if (transactionStack.isEmpty()) {
            System.out.println("No transactions in this session.");
            return;
        }

        int index = 1;
        for (int i = transactionStack.size() - 1; i >= 0; i--) {
            System.out.println(index + ". " + transactionStack.get(i));
            index++;
        }
    }

    static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }
}
