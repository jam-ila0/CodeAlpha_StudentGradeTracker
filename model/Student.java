
package model;

public class Student {

    private String id;
    private String name;
    private String email;
    private String className;

    // Constructor
    public Student(String id, String name, String email, String className) {
        this.id = id;
        this.name = name;
        this.email = (email != null) ? email : "N/A";
        this.className = (className != null) ? className : "Tronc Commun";
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getClassName() {
        return className;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
