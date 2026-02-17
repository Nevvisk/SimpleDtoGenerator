package org.example.entity;

import org.example.annotation.GenerateDto;

@GenerateDto(pkg = "org.example.entity", name ="UserDto")
public class UserEntity {
    String name;
    String something;

    public String getSomething() {
        return something;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSomething(String something) {
        this.something = something;
    }
}
