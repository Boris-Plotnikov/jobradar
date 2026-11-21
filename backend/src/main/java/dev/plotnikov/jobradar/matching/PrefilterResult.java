package dev.plotnikov.jobradar.matching;

public record PrefilterResult(boolean passed, String reason, String direction) {

    public static PrefilterResult pass(String direction) {
        return new PrefilterResult(true, null, direction);
    }

    public static PrefilterResult reject(String reason) {
        return new PrefilterResult(false, reason, null);
    }
}
