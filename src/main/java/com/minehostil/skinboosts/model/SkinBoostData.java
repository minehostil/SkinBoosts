package com.minehostil.skinboosts.model;

public class SkinBoostData {

    private final int customModelData;
    private final String name;
    private final double essenceMultiplier;
    private final double moneyMultiplier;
    private final double toolXpMultiplier;
    private final double cyberXpMultiplier;

    public SkinBoostData(int customModelData, String name,
                         double essenceMultiplier, double moneyMultiplier,
                         double toolXpMultiplier, double cyberXpMultiplier) {
        this.customModelData = customModelData;
        this.name = name;
        this.essenceMultiplier = essenceMultiplier;
        this.moneyMultiplier = moneyMultiplier;
        this.toolXpMultiplier = toolXpMultiplier;
        this.cyberXpMultiplier = cyberXpMultiplier;
    }

    public int getCustomModelData()    { return customModelData; }
    public String getName()            { return name; }
    public double getEssenceMultiplier() { return essenceMultiplier; }
    public double getMoneyMultiplier()   { return moneyMultiplier; }
    public double getToolXpMultiplier()  { return toolXpMultiplier; }
    public double getCyberXpMultiplier() { return cyberXpMultiplier; }

    @Override
    public String toString() {
        return String.format("SkinBoostData{cmd=%d, name='%s', essence=%.2fx, money=%.2fx, toolXp=%.2fx, cyberXp=%.2fx}",
                customModelData, name, essenceMultiplier, moneyMultiplier, toolXpMultiplier, cyberXpMultiplier);
    }
}