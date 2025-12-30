package yea.ecomservapi.kernel.valueobject;

public record Ruc(String value) {

    public Ruc {
        // Validar que no sea nulo ni vacío
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El RUC no puede ser nulo o vacío");
        }
        // Validar que tenga exactamente 11 caracteres
        if (value.length() != 11) {
            throw new IllegalArgumentException("El RUC debe tener exactamente 11 caracteres");
        }
        // Validar que solo contenga dígitos
        if (!value.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener solo dígitos");
        }
        // Validar que empiece con 10 o 20
        if (!value.startsWith("10") && !value.startsWith("20")) {
            throw new IllegalArgumentException("El RUC debe empezar con 10 o 20");
        }
    }
}
