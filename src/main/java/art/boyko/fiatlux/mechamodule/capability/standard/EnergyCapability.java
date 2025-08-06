package art.boyko.fiatlux.mechamodule.capability.standard;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.capability.ConnectionType;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Унифицированная энергетическая capability для всех модулей
 */
public class EnergyCapability implements IModuleCapability {
    
    private final IMechaModule owner;
    private final EnergyProvider energyProvider;
    
    public EnergyCapability(IMechaModule owner, EnergyProvider energyProvider) {
        this.owner = owner;
        this.energyProvider = energyProvider;
    }
    
    @Override
    public ResourceLocation getCapabilityId() {
        return ResourceLocation.fromNamespaceAndPath("fiatlux", "energy");
    }
    
    @Override
    public IMechaModule getOwnerModule() {
        return owner;
    }
    
    @Override
    public boolean canConnectTo(Direction direction, IModuleCapability other) {
        // Энергия может соединяться только с другими энергетическими capabilities
        return other instanceof EnergyCapability;
    }
    
    @Override
    public void onConnectionEstablished(Direction direction, IModuleCapability other, ModuleConnection connection) {
        // Connection established - ready for energy transfer
    }
    
    @Override
    public void onConnectionBroken(Direction direction, IModuleCapability other) {
        // Connection broken - stop energy transfer
    }
    
    @Override
    public boolean needsTicking() {
        return false; // Энергетическое управление происходит на уровне модулей
    }
    
    @Override
    public boolean supportsConnectionType(ConnectionType connectionType) {
        return connectionType.canTransferEnergy();
    }
    
    // Энергетические методы
    
    /**
     * Получить энергию от другого источника
     * @param maxReceive Максимальное количество энергии для получения
     * @param simulate Если true, только симулировать, не фактически получать
     * @return Количество фактически полученной энергии
     */
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return energyProvider.receiveEnergy(maxReceive, simulate);
    }
    
    /**
     * Извлечь энергию для передачи другому модулю
     * @param maxExtract Максимальное количество энергии для извлечения
     * @param simulate Если true, только симулировать, не фактически извлекать
     * @return Количество фактически извлеченной энергии
     */
    public int extractEnergy(int maxExtract, boolean simulate) {
        return energyProvider.extractEnergy(maxExtract, simulate);
    }
    
    /**
     * Получить текущее количество хранящейся энергии
     */
    public int getEnergyStored() {
        return energyProvider.getEnergyStored();
    }
    
    /**
     * Получить максимальную емкость энергии
     */
    public int getMaxEnergyStored() {
        return energyProvider.getMaxEnergyStored();
    }
    
    /**
     * Может ли этот модуль получать энергию
     */
    public boolean canReceive() {
        return energyProvider.canReceive();
    }
    
    /**
     * Может ли этот модуль отдавать энергию
     */
    public boolean canExtract() {
        return energyProvider.canExtract();
    }
    
    /**
     * Интерфейс для модулей, которые хотят предоставить энергетическую capability
     */
    public interface EnergyProvider {
        int receiveEnergy(int maxReceive, boolean simulate);
        int extractEnergy(int maxExtract, boolean simulate);
        int getEnergyStored();
        int getMaxEnergyStored();
        boolean canReceive();
        boolean canExtract();
    }
}