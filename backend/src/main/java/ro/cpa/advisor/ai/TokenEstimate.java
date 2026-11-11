package ro.cpa.advisor.ai;

/**
 * Estimare grosiera a tokenilor (~4 caractere/token) pentru cazurile in care furnizorul nu raporteaza consumul sau
 * fluxul a fost intrerupt. E mai bine sa numaram aproximativ decat sa lasam o intrerupere sa fie gratuita.
 */
final class TokenEstimate {

    private TokenEstimate() {}

    static int of(int chars) {
        return Math.max(1, (chars + 3) / 4);
    }
}
