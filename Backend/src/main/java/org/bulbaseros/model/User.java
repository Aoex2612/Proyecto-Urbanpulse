package org.bulbaseros.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad de dominio JPA que representa a un usuario en la plataforma UrbanPulse.
 * <p>
 * Mapea la tabla {@code users} de la base de datos PostgreSQL, almacenando credenciales,
 * nombre completo, rol asignado dentro del sistema y fecha de creación.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Identificador único secuencial del usuario (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Correo electrónico único del usuario, utilizado como identificador de inicio de sesión.
     * Longitud máxima de 150 caracteres.
     */
    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Hash seguro de la contraseña generado mediante el algoritmo BCrypt.
     * Longitud máxima de 255 caracteres.
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Nombre completo y apellidos del usuario.
     * Longitud máxima de 150 caracteres.
     */
    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Rol funcional asignado al usuario dentro de la plataforma.
     * Por defecto se inicializa con el valor {@link UserRole#CITIZEN}.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.CITIZEN;

    /**
     * Identificador opcional del departamento municipal al que pertenece el usuario.
     * Aplica principalmente a operadores y técnicos (nullable).
     */
    @Column(name = "department_id")
    private Long departmentId;

    /**
     * Marca de tiempo de registro del usuario generada automáticamente por Hibernate al persistir.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el rol por defecto en {@link UserRole#CITIZEN}.
     */
    public User() {
        this.role = UserRole.CITIZEN;
    }

    /**
     * Constructor para la creación básica de un usuario ciudadano.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     */
    public User(String email, String passwordHash, String fullName) {
        this(email, passwordHash, fullName, UserRole.CITIZEN, null);
    }

    /**
     * Constructor para la creación de un usuario con un rol específico.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     * @param role         Rol asignado al usuario; si es nulo se asigna {@link UserRole#CITIZEN}.
     */
    public User(String email, String passwordHash, String fullName, UserRole role) {
        this(email, passwordHash, fullName, role, null);
    }

    /**
     * Constructor completo para la creación de un usuario en el sistema.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     * @param role         Rol asignado al usuario; si es nulo se asigna {@link UserRole#CITIZEN}.
     * @param departmentId Identificador del departamento municipal opcional.
     */
    public User(String email, String passwordHash, String fullName, UserRole role, Long departmentId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = (role != null) ? role : UserRole.CITIZEN;
        this.departmentId = departmentId;
    }

    /**
     * Obtiene el identificador único del usuario.
     *
     * @return Identificador primario como {@link Long}, o {@code null} si aún no ha sido persistido.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único del usuario.
     *
     * @param id Identificador primario como {@link Long}.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Cadena con el correo electrónico.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param email Dirección de correo electrónico (máximo 150 caracteres).
     * @throws IllegalArgumentException si la longitud del correo supera los 150 caracteres.
     */
    public void setEmail(String email) {
        if (email != null && email.length() > 150) {
            throw new IllegalArgumentException("El correo electrónico no puede superar los 150 caracteres");
        }
        this.email = email;
    }

    /**
     * Obtiene el hash de la contraseña del usuario.
     *
     * @return Cadena con el hash BCrypt de la contraseña.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Establece el hash de la contraseña del usuario.
     *
     * @param passwordHash Cadena con el hash BCrypt (máximo 255 caracteres).
     * @throws IllegalArgumentException si la longitud del hash supera los 255 caracteres.
     */
    public void setPasswordHash(String passwordHash) {
        if (passwordHash != null && passwordHash.length() > 255) {
            throw new IllegalArgumentException("El hash de contraseña no puede superar los 255 caracteres");
        }
        this.passwordHash = passwordHash;
    }

    /**
     * Obtiene el nombre completo del usuario.
     *
     * @return Cadena con el nombre completo.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Establece el nombre completo del usuario.
     *
     * @param fullName Cadena con el nombre completo (máximo 150 caracteres).
     * @throws IllegalArgumentException si la longitud del nombre supera los 150 caracteres.
     */
    public void setFullName(String fullName) {
        if (fullName != null && fullName.length() > 150) {
            throw new IllegalArgumentException("El nombre completo no puede superar los 150 caracteres");
        }
        this.fullName = fullName;
    }

    /**
     * Obtiene el rol asignado al usuario.
     *
     * @return Instancia de {@link UserRole}.
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Establece el rol asignado al usuario.
     * Si el valor proporcionado es {@code null}, se asigna por defecto {@link UserRole#CITIZEN}.
     *
     * @param role Rol del usuario.
     */
    public void setRole(UserRole role) {
        this.role = (role != null) ? role : UserRole.CITIZEN;
    }

    /**
     * Obtiene el identificador del departamento municipal asignado.
     *
     * @return Identificador del departamento o {@code null} si no está asignado.
     */
    public Long getDepartmentId() {
        return departmentId;
    }

    /**
     * Establece el identificador del departamento municipal asignado.
     *
     * @param departmentId Identificador del departamento o {@code null}.
     */
    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * Obtiene la marca de tiempo de creación del registro.
     *
     * @return {@link OffsetDateTime} con la fecha y hora de creación.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la marca de tiempo de creación del registro.
     *
     * @param createdAt Fecha y hora de creación como {@link OffsetDateTime}.
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (id != null && user.id != null) {
            return Objects.equals(id, user.id);
        }
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return (id != null) ? Objects.hash(id) : Objects.hash(email);
    }

    /**
     * Representación textual de la entidad de usuario omitiendo deliberadamente
     * el hash de la contraseña por motivos de seguridad.
     *
     * @return Cadena descriptiva con los atributos no confidenciales del usuario.
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", departmentId=" + departmentId +
                ", createdAt=" + createdAt +
                '}';
    }
}
