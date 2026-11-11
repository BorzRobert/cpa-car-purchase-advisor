package ro.cpa.advisor.domain;

/** Bugetul poate fi o suma totala sau o rata lunara; fara acest marcaj o cifra din profil ar fi ambigua. */
public enum BudgetKind {
    TOTAL,
    MONTHLY
}
