package com.openclassrooms.p2dfsbea.ui;

import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;
import com.openclassrooms.p2dfsbea.service.ExpenseService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.math.RoundingMode;
import java.util.Map;

public class ConsoleMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final ExpenseRepository repository = new ExpenseRepository();
    private final ExpenseService service = new ExpenseService(repository);

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> showHistory();
                case "2" -> addExpense();
                case "3" -> showStats();
                case "4" -> simulateCar();
                case "5" -> {
                    System.out.println("Goodbye");
                    running = false;
                }
                default -> System.out.println("Unknown option, please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Personal Finance Tracker ---");
        System.out.println("1) Show history");
        System.out.println("2) Add expense");
        System.out.println("3) Statistics");
        System.out.println("4) Car simulation");
        System.out.println("5) Quit");
        System.out.print("Choose an option: ");
    }

    private void showHistory() {
        List<Expense> list = service.loadAllExpenses();
        if (list.isEmpty()) {
            System.out.println("No expenses found.");
            return;
        }
        System.out.println("\nHistory (most recent first):");
        list.stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .forEach(e -> System.out.println(e.toCsvLine()));
        System.out.println("---");
        System.out.println("Total: " + service.getGlobalTotal(list) + " €");
    }

    private void addExpense() {
        try {
            System.out.print("Date (dd/MM/yyyy): ");
            String dateS = scanner.nextLine().trim();
            LocalDate date = LocalDate.parse(dateS, Expense.FORMATTER);

            System.out.print("Category: ");
            String category = scanner.nextLine().trim();

            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            System.out.print("Amount (e.g. 12.50): ");
            String amountS = scanner.nextLine().trim().replace(',', '.');
            BigDecimal amount = new BigDecimal(amountS);

            Expense e = new Expense(date, category, description, amount);
            service.addExpense(e);
            System.out.println("Expense added.");
        } catch (DateTimeParseException dtpe) {
            System.out.println("Invalid date format. Use dd/MM/yyyy.");
        } catch (IOException io) {
            System.out.println("Unable to save expense: " + io.getMessage());
        } catch(NumberFormatException ex) {
            System.out.println("Invalid amount format. Decimal is expected.");
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private void showStats() {
        List<Expense> list = service.loadAllExpenses();
        if (list.isEmpty()) {
            System.out.println("No expenses found.");
            return;
        }

        BigDecimal total = service.getGlobalTotal(list);
        long months = service.countDistinctMonths(list);
        BigDecimal avgMonthly = service.getAvgMonthlyExpenses(list);
        Map<String, BigDecimal> categoryTotals = service.getCategoryTotals(list);

        System.out.println("\n--- Statistics ---");
        System.out.println("Total expenses : " + total + " €");
        System.out.println("Period         : " + months + " month(s)");
        System.out.println("Monthly average: " + avgMonthly + " €");
        System.out.println("\nBy category:");
        System.out.printf("  %-15s %10s %14s %9s%n", "Category", "Total (€)", "Avg/month (€)", "Share");
        System.out.println("  " + "-".repeat(53));
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            BigDecimal catTotal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal catAvg = catTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            BigDecimal share = catTotal.multiply(new BigDecimal("100"))
                    .divide(total, 1, RoundingMode.HALF_UP);
            System.out.printf("  %-15s %10s %14s %8s%%%n",
                    entry.getKey(), catTotal, catAvg, share);
        }
    }


    private void simulateCar() {
        try {
            System.out.print("Monthly income (€): ");
            BigDecimal income = new BigDecimal(scanner.nextLine().trim().replace(',', '.'));

            System.out.print("Car price (€): ");
            BigDecimal carPrice = new BigDecimal(scanner.nextLine().trim().replace(',', '.'));

            List<Expense> list = service.loadAllExpenses();
            BigDecimal avgMonthly = service.getAvgMonthlyExpenses(list);
            BigDecimal monthlySavings = income.subtract(avgMonthly).setScale(2, RoundingMode.HALF_UP);

            System.out.println("\n--- Car Simulation ---");
            System.out.println("Average monthly expenses: " + avgMonthly + " €");
            System.out.println("Monthly savings         : " + monthlySavings + " €");

            int months = service.monthsToSaveCar(list, income, carPrice);
            if (months < 0) {
                System.out.println("Insufficient savings: your expenses exceed your income get new job please.");
            } else {
                System.out.println("Months needed to save " + carPrice + " €: " + months
                        + " (" + (months / 12) + " year(s) and " + (months % 12) + " month(s))");
            }
        } catch (NumberFormatException ex) {
            System.out.println("Invalid amount format.");
        }
    }
}