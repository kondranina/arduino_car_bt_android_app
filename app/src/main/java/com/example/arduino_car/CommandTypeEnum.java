package com.example.arduino_car;

public enum CommandTypeEnum {
    LED_ON(0),
    LED_OFF(1);

    private int value;
    CommandTypeEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
