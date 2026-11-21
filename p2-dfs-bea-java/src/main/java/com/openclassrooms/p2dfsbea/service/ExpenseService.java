package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;

import java.io.IOException;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ExpenseService {
    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public List<Expense> loadAllExpenses() {
        return repository.readAll();
    }

    public void addExpense(Expense expense) throws IOException {
        repository.append(expense);
    }

//    somme de toutes les dépenses

    public BigDecimal getGlobalTotal(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

//     nombre de mois couverts par les dépenses

    public long countDistinctMonths(List<Expense> expenses) {
        return expenses.stream()
                .map(e -> e.getDate().getYear() * 12 + e.getDate().getMonthValue())
                .distinct()
                .count();
    }

//    total par catégorie du plus élevé au plus faible

    public Map<String, BigDecimal> getCategoryTotals(List<Expense> expenses) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : expenses) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        // Tri des entrées par total décroissant
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(totals.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

//      map ordonnée

        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

//     moyenne mensuelle des dépenses

    public BigDecimal getAvgMonthlyExpenses(List<Expense> expenses) {
        long months = countDistinctMonths(expenses);
        if (months == 0) return BigDecimal.ZERO;
        return getGlobalTotal(expenses).divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }


    /** Achat voiture **/

    // Calcule le nombre de mois nécessaires pour épargner le prix de la voiture.
    // Épargne mensuelle = revenu mensuel − moyenne mensuelle des dépenses.
    // Retourne -1 si l'épargne est nulle ou négative.

    public int monthsToSaveCar(List<Expense> expenses, BigDecimal monthlyIncome, BigDecimal carPrice) {
        BigDecimal monthlySavings = monthlyIncome.subtract(getAvgMonthlyExpenses(expenses));
        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) return -1;
        double months = Math.ceil(carPrice.doubleValue() / monthlySavings.doubleValue());
        return (int) months;
    }


    /**Pret immobilier**/

//     calcule la capacité d'emprunt avec un taux d'endettement de 35 %.
//     formule : P = M × (1 − (1+r)^−n) / r
//     M = revenu × 35 %, r = taux annuel / 12 / 100, n = durée (mois)

    public BigDecimal mortgageCapacity(BigDecimal monthlyIncome, double annualRatePct, int durationMonths) {
        BigDecimal maxPayment = monthlyIncome.multiply(new BigDecimal("0.35"))
                .setScale(2, RoundingMode.HALF_UP);
        double r = annualRatePct / 100.0 / 12.0;
        double M = maxPayment.doubleValue();
        double P = (r == 0) ? M * durationMonths : M * (1 - Math.pow(1 + r, -durationMonths)) / r;
        return BigDecimal.valueOf(P).setScale(2, RoundingMode.HALF_UP);
    }
}