package com.lexis.speedometer;

public interface IDataProvider {
    void registerListener(int channel, IDataListener listener);
    void unregisterListener(int channel, IDataListener listener);
}
