package com.thundercomm.eBox.Data;

public class ObjectData {
    public float[] rect;
    public int age;
    public String gender;

    public ObjectData() {
        age = 0;
        gender = "M";
        rect = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    }

    public String toString() {
        return  age + "," + gender;
    }
}
