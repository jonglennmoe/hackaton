package se.hildur.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A hotel employee who can log in to the staff view. The PIN is stored only as a salted hash. */
@Entity
public class StaffUser {

    @Id
    private String username;
    private String displayName;
    private String roleSv;
    private String roleEn;
    private String pinSalt;
    private String pinHash;

    protected StaffUser() {
    }

    public StaffUser(String username, String displayName, String roleSv, String roleEn, String pinSalt, String pinHash) {
        this.username = username;
        this.displayName = displayName;
        this.roleSv = roleSv;
        this.roleEn = roleEn;
        this.pinSalt = pinSalt;
        this.pinHash = pinHash;
    }

    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRoleSv() { return roleSv; }
    public String getRoleEn() { return roleEn; }
    public String getPinSalt() { return pinSalt; }
    public String getPinHash() { return pinHash; }
}
