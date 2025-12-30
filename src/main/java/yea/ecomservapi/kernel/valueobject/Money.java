package yea.ecomservapi.kernel.valueobject;

import java.math.BigDecimal;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        if (currency == null) {
            throw new IllegalArgumentException("La moneda no puede ser nula");
        }
    }

    public  enum Currency{
        PEN, USD
    }

    //Crear Money en soles facilmente
    public static Money soles(BigDecimal amount) {
        return new Money(amount, Currency.PEN);
    }

    //Sumar dos Money (deben ser de la misma monedad)
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("No se pueden sumar montos de diferentes monedas");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    //Calcular el  IGV (18%)
    public Money calculateIgv() {
        BigDecimal igvRate = new BigDecimal("0.18");
        BigDecimal igvAmount = this.amount.multiply(igvRate);
        return new Money(igvAmount, this.currency);
    }

    //Restar dos Money (deben ser de la misma monedad)
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("No se pueden restar montos de diferentes monedas");
        }

        BigDecimal resultAmount = this.amount.subtract(other.amount);
        if (resultAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El resultado no puede ser negativo");
        }

        return new Money(resultAmount, this.currency);
    }
}
