package demo.pharma.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String type, Object id) {
        super(type + " not found: " + id);
    }
}
