import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

class BankAccount {
    private final String accountNumber;
    private final int pin;
    private double balance;
    private final String accountHolder;

    public BankAccount(String accountNumber, int pin, double initialBalance, String accountHolder) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = initialBalance;
        this.accountHolder = accountHolder;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public int getPin() {
        return pin;
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void deposit(double amount) {
        this.balance += amount;
    }

    public boolean withdraw(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Account: %s, Holder: %s, Balance: $%.2f", accountNumber, accountHolder, balance);
    }
}

class Bank {
    private final Map<String, BankAccount> accounts;
    private BankAccount currentAccount = null;

    public Bank() {
        this.accounts = new HashMap<>();
        accounts.put("123456", new BankAccount("123456", 1111, 500.00, "Arjit Singh"));
        accounts.put("987654", new BankAccount("987654", 2222, 1250.75, "Kartik"));
    }

    public boolean login(String accNum, String pin) {
        try {
            int parsedPin = Integer.parseInt(pin);
            BankAccount account = accounts.get(accNum);
            if (account != null && account.getPin() == parsedPin) {
                currentAccount = account;
                return true;
            }
        } catch (NumberFormatException e) {
        }
        currentAccount = null;
        return false;
    }

    public void logout() {
        currentAccount = null;
    }

    public BankAccount getCurrentAccount() {
        return currentAccount;
    }

    public boolean deposit(double amount) {
        if (currentAccount != null && amount > 0) {
            currentAccount.deposit(amount);
            return true;
        }
        return false;
    }

    public String withdraw(double amount) {
        if (currentAccount == null) return "No active session.";
        if (amount <= 0) return "Withdrawal amount must be positive.";

        if (currentAccount.withdraw(amount)) {
            return "SUCCESS";
        } else {
            return "Insufficient funds.";
        }
    }

    public String transfer(String targetAccNum, double amount) {
        if (currentAccount == null) return "No active session.";
        if (amount <= 0) return "Transfer amount must be positive.";

        BankAccount targetAccount = accounts.get(targetAccNum);
        if (targetAccount == null) return "Target account not found.";

        if (currentAccount.withdraw(amount)) {
            targetAccount.deposit(amount);
            return "SUCCESS";
        } else {
            return "Insufficient funds for transfer.";
        }
    }
}

public class BankingApp extends JFrame implements ActionListener {
    private final Bank bank = new Bank();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final JLabel statusLabel = new JLabel("Welcome to the Bank!", SwingConstants.CENTER);

    private JTextField loginAccField;
    private JPasswordField loginPinField;

    private JLabel currentBalanceLabel;
    private JTextField depositAmountField;
    private JTextField withdrawAmountField;
    private JTextField transferAccField;
    private JTextField transferAmountField;

    private static final String LOGIN_PANEL = "Login";
    private static final String ACCOUNT_PANEL = "Account";
    private static final String DEPOSIT_PANEL = "Deposit";
    private static final String WITHDRAW_PANEL = "Withdraw";
    private static final String TRANSFER_PANEL = "Transfer";

    // Define consistent colors for a cleaner look
    private static final Color PRIMARY_BLUE = new Color(30, 144, 255);
    private static final Color BACKGROUND_LIGHT = new Color(248, 248, 255);
    private static final Color SUCCESS_GREEN = new Color(34, 139, 34);

    public BankingApp() {
        setTitle("Simple In-Memory Bank");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 550);
        setLocationRelativeTo(null);

        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(PRIMARY_BLUE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        statusLabel.setBackground(new Color(230, 230, 250));
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.NORTH);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setBackground(BACKGROUND_LIGHT);
        add(mainPanel, BorderLayout.CENTER);

        mainPanel.add(createLoginPanel(), LOGIN_PANEL);
        mainPanel.add(createAccountPanel(), ACCOUNT_PANEL);
        mainPanel.add(createDepositPanel(), DEPOSIT_PANEL);
        mainPanel.add(createWithdrawPanel(), WITHDRAW_PANEL);
        mainPanel.add(createTransferPanel(), TRANSFER_PANEL);

        cardLayout.show(mainPanel, LOGIN_PANEL);
        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 15, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Secure Account Login", 
                javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, 
                new Font("Arial", Font.BOLD, 16), PRIMARY_BLUE));

        JPanel accPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        accPanel.setBackground(Color.WHITE);
        accPanel.add(new JLabel("Account Number:"));
        loginAccField = new JTextField(15);
        accPanel.add(loginAccField);

        JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        pinPanel.setBackground(Color.WHITE);
        pinPanel.add(new JLabel("PIN:"));
        loginPinField = new JPasswordField(15);
        pinPanel.add(loginPinField);

        panel.add(accPanel);
        panel.add(pinPanel);
        
        JButton loginButton = new JButton("Login");
        loginButton.setActionCommand("LOGIN");
        loginButton.addActionListener(this);
        loginButton.setBackground(PRIMARY_BLUE);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setBackground(Color.WHITE);
        buttonWrapper.add(loginButton);

        panel.add(buttonWrapper);

        return panel;
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_LIGHT);

        JPanel balancePanel = new JPanel(new GridLayout(3, 1, 10, 10));
        balancePanel.setBackground(BACKGROUND_LIGHT);
        balancePanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));

        balancePanel.add(new JLabel("Current Balance:", SwingConstants.CENTER));
        currentBalanceLabel = new JLabel("$0.00", SwingConstants.CENTER);
        currentBalanceLabel.setFont(new Font("Arial", Font.BOLD, 40));
        currentBalanceLabel.setForeground(SUCCESS_GREEN);
        balancePanel.add(currentBalanceLabel);
        balancePanel.add(new JLabel("What would you like to do?", SwingConstants.CENTER));

        panel.add(balancePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        buttonPanel.setBackground(BACKGROUND_LIGHT);

        String[] buttonLabels = {"Deposit", "Withdraw", "Transfer", "Logout"};
        String[] commands = {"DEPOSIT_VIEW", "WITHDRAW_VIEW", "TRANSFER_VIEW", "LOGOUT"};

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton(buttonLabels[i]);
            button.setActionCommand(commands[i]);
            button.addActionListener(this);
            button.setBackground(new Color(192, 192, 192)); // Silver
            button.setFont(new Font("Arial", Font.PLAIN, 16));
            button.setForeground(Color.DARK_GRAY);
            buttonPanel.add(button);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDepositPanel() {
        // Create the specific field first
        depositAmountField = new JTextField(15);
        // Pass it to the helper method
        return createTransactionBasePanel("Deposit Funds", "DEPOSIT_ACTION", PRIMARY_BLUE, depositAmountField);
    }

    private JPanel createWithdrawPanel() {
        // Create the specific field first
        withdrawAmountField = new JTextField(15);
        // Pass it to the helper method
        return createTransactionBasePanel("Withdraw Funds", "WITHDRAW_ACTION", new Color(255, 69, 0), withdrawAmountField);
    }

    private JPanel createTransferPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_LIGHT);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 100, 100)), "Transfer Funds",
                javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), new Color(255, 100, 100)));

        JPanel inputPanel = new JPanel(new GridLayout(3, 1, 10, 15));
        inputPanel.setBackground(BACKGROUND_LIGHT);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JPanel targetAccWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        targetAccWrapper.setBackground(BACKGROUND_LIGHT);
        targetAccWrapper.add(new JLabel("Target Account Number:"));
        transferAccField = new JTextField(15);
        targetAccWrapper.add(transferAccField);

        JPanel amountWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        amountWrapper.setBackground(BACKGROUND_LIGHT);
        amountWrapper.add(new JLabel("Amount to Transfer ($):"));
        transferAmountField = new JTextField(15);
        amountWrapper.add(transferAmountField);
        
        inputPanel.add(targetAccWrapper);
        inputPanel.add(amountWrapper);

        JButton transferButton = createActionButton("Transfer", "TRANSFER_ACTION", new Color(255, 100, 100));
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setBackground(BACKGROUND_LIGHT);
        buttonWrapper.add(transferButton);
        inputPanel.add(buttonWrapper);


        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(createBackButton(), BorderLayout.SOUTH);
        return panel;
    }

    // Updated helper method to accept the JTextField instance
    private JPanel createTransactionBasePanel(String title, String actionCommand, Color actionColor, JTextField amountField) {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BACKGROUND_LIGHT);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(actionColor), title,
                javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), actionColor));

        JPanel inputPanel = new JPanel(new GridLayout(2, 1, 10, 15));
        inputPanel.setBackground(BACKGROUND_LIGHT);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));

        JPanel amountWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        amountWrapper.setBackground(BACKGROUND_LIGHT);
        amountWrapper.add(new JLabel("Amount ($):"));
        
        // Use the passed-in field instead of creating a new one or casting later
        amountWrapper.add(amountField);
        inputPanel.add(amountWrapper);
        
        JButton actionButton = createActionButton(
                title.split(" ")[0],
                actionCommand,
                actionColor
        );

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setBackground(BACKGROUND_LIGHT);
        buttonWrapper.add(actionButton);
        inputPanel.add(buttonWrapper);

        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(createBackButton(), BorderLayout.SOUTH);
        return panel;
    }

    private JButton createActionButton(String label, String command, Color color) {
        JButton button = new JButton(label);
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        return button;
    }

    private JButton createBackButton() {
        JButton backButton = new JButton("Back to Account");
        backButton.setActionCommand("BACK");
        backButton.addActionListener(this);
        backButton.setBackground(new Color(150, 150, 150));
        backButton.setForeground(Color.WHITE);
        return backButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        try {
            switch (command) {
                case "LOGIN":
                    handleLogin();
                    break;
                case "LOGOUT":
                    handleLogout();
                    break;
                case "BACK":
                    showAccountView("Operation canceled.");
                    break;
                case "DEPOSIT_VIEW":
                    depositAmountField.setText("");
                    cardLayout.show(mainPanel, DEPOSIT_PANEL);
                    setStatus("Enter amount to deposit.");
                    break;
                case "WITHDRAW_VIEW":
                    withdrawAmountField.setText("");
                    cardLayout.show(mainPanel, WITHDRAW_PANEL);
                    setStatus("Enter amount to withdraw.");
                    break;
                case "TRANSFER_VIEW":
                    transferAccField.setText("");
                    transferAmountField.setText("");
                    cardLayout.show(mainPanel, TRANSFER_PANEL);
                    setStatus("Enter transfer details.");
                    break;
                case "DEPOSIT_ACTION":
                    handleDeposit();
                    break;
                case "WITHDRAW_ACTION":
                    handleWithdraw();
                    break;
                case "TRANSFER_ACTION":
                    handleTransfer();
                    break;
            }
        } catch (Exception ex) {
            setStatus("An unexpected error occurred: " + ex.getMessage(), Color.RED);
        }
    }

    private void handleLogin() {
        String accNum = loginAccField.getText().trim();
        String pin = new String(loginPinField.getPassword());

        if (bank.login(accNum, pin)) {
            showAccountView("Login successful. Welcome, " + bank.getCurrentAccount().getAccountHolder() + "!");
        } else {
            setStatus("Login failed. Check Account Number and PIN.", Color.RED);
            loginAccField.setText("");
            loginPinField.setText("");
        }
    }

    private void handleLogout() {
        bank.logout();
        loginAccField.setText("");
        loginPinField.setText("");
        cardLayout.show(mainPanel, LOGIN_PANEL);
        setStatus("You have been logged out. Thank you for banking with us.");
    }

    private void handleDeposit() {
        try {
            double amount = Double.parseDouble(depositAmountField.getText());
            if (amount <= 0) {
                setStatus("Deposit amount must be positive.", Color.ORANGE);
                return;
            }
            if (bank.deposit(amount)) {
                showAccountView(String.format("Successfully deposited $%.2f.", amount));
            } else {
                setStatus("Deposit failed. Check your session.", Color.RED);
            }
        } catch (NumberFormatException ex) {
            setStatus("Invalid amount entered.", Color.RED);
        }
    }

    private void handleWithdraw() {
        try {
            double amount = Double.parseDouble(withdrawAmountField.getText());
            String result = bank.withdraw(amount);

            if ("SUCCESS".equals(result)) {
                showAccountView(String.format("Successfully withdrew $%.2f.", amount));
            } else if ("Insufficient funds.".equals(result)) {
                setStatus(result, Color.RED);
            } else {
                setStatus(result, Color.RED);
                handleLogout();
            }

        } catch (NumberFormatException ex) {
            setStatus("Invalid amount entered.", Color.RED);
        }
    }

    private void handleTransfer() {
        try {
            String targetAcc = transferAccField.getText().trim();
            double amount = Double.parseDouble(transferAmountField.getText());

            if (targetAcc.isEmpty()) {
                setStatus("Target account number cannot be empty.", Color.ORANGE);
                return;
            }

            String result = bank.transfer(targetAcc, amount);

            if ("SUCCESS".equals(result)) {
                showAccountView(String.format("Successfully transferred $%.2f to account %s.", amount, targetAcc));
            } else if ("Insufficient funds for transfer.".equals(result)) {
                setStatus(result, Color.RED);
            } else {
                setStatus(result, Color.RED);
            }

        } catch (NumberFormatException ex) {
            setStatus("Invalid amount entered.", Color.RED);
        }
    }

    private void showAccountView(String statusMessage) {
        BankAccount account = bank.getCurrentAccount();
        if (account != null) {
            currentBalanceLabel.setText(String.format("$%.2f", account.getBalance()));
            setStatus(statusMessage, SUCCESS_GREEN);
            cardLayout.show(mainPanel, ACCOUNT_PANEL);
        } else {
            handleLogout();
        }
    }

    private void setStatus(String message) {
        setStatus(message, PRIMARY_BLUE);
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
        }
        SwingUtilities.invokeLater(BankingApp::new);
    }
}