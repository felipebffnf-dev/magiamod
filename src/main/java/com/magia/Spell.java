package com.magia;

public enum Spell {
    BOLA_DE_FOGO("Bola de Fogo", 20f, 15),
    RAIO("Raio", 30f, 40),
    CURA("Cura", 25f, 60);

    public final String nome;
    public final float custo;
    public final int cooldown; // em ticks (20 ticks = 1 segundo)

    Spell(String nome, float custo, int cooldown) {
        this.nome = nome;
        this.custo = custo;
        this.cooldown = cooldown;
    }

    public Spell proxima() {
        Spell[] todas = values();
        return todas[(ordinal() + 1) % todas.length];
    }
}
