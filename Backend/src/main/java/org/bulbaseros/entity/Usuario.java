package org.bulbaseros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * nombre completo, rol asignado dentro del sistema, relación con departamento municipal
 * y fecha de creación.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Entity
@Table(name = "users")
public class Usuario {

    /**
     * Longitud máxima permitida para la dirección de correo electrónico.
     */
    public static final int MAX_EMAIL_LENGTH = 150;

    /**
     * Longitud máxima permitida para el hash de la contraseña.
     */
    public static final int MAX_PASSWORD_HASH_LENGTH = 255;

    /**
     * Longitud máxima permitida para el nombre completo del usuario.
     */
    public static final int MAX_FULL_NAME_LENGTH = 150;

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
    @Size(max = MAX_EMAIL_LENGTH)
    @Column(name = "email", nullable = false, unique = true, length = MAX_EMAIL_LENGTH)
    private String email;

    /**
     * Hash seguro de la contraseña generado mediante el algoritmo BCrypt.
     * Longitud máxima de 255 caracteres.
     */
    @NotBlank
    @Size(max = MAX_PASSWORD_HASH_LENGTH)
    @Column(name = "password_hash", nullable = false, length = MAX_PASSWORD_HASH_LENGTH)
    private String passwordHash;

    /**
     * Nombre completo y apellidos del usuario.
     * Longitud máxima de 150 caracteres.
     */
    @NotBlank
    @Size(max = MAX_FULL_NAME_LENGTH)
    @Column(name = "full_name", nullable = false, length = MAX_FULL_NAME_LENGTH)
    private String fullName;

    /**
     * Rol funcional asignado al usuario dentro de la plataforma.
     * Por defecto se inicializa con el valor {@link RolUsuario#CITIZEN}.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RolUsuario role = RolUsuario.CITIZEN;

    /**
     * Departamento municipal asignado al usuario (opcional, aplicable a operadores y técnicos).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Departamento department;

    /**
     * Marca de tiempo de registro del usuario generada automáticamente por Hibernate al persistir.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa el rol por defecto en {@link RolUsuario#CITIZEN}.
     */
    public Usuario() {
        this.role = RolUsuario.CITIZEN;
    }

    /**
     * Constructor para la creación básica de un usuario ciudadano.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     */
    public Usuario(String email, String passwordHash, String fullName) {
        this(email, passwordHash, fullName, RolUsuario.CITIZEN, (Departamento) null);
    }

    /**
     * Constructor para la creación de un usuario con un rol específico.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     * @param role         Rol asignado al usuario; si es nulo se asigna {@link RolUsuario#CITIZEN}.
     */
    public Usuario(String email, String passwordHash, String fullName, RolUsuario role) {
        this(email, passwordHash, fullName, role, (Departamento) null);
    }

    /**
     * Constructor completo para la creación de un usuario con departamento asignado.
     *
     * @param email        Correo electrónico único del usuario.
     * @param passwordHash Hash de la contraseña codificada con BCrypt.
     * @param fullName     Nombre completo del usuario.
     * @param role         Rol asignado al usuario; si es nulo se asigna {@link RolUsuario#CITIZEN}.
     * @param department   Entidad del departamento municipal asignado (opcional).
     */
    public Usuario(String email, String passwordHash, String fullName, RolUsuario role, Departamento department) {
        setEmail(email);
        setPasswordHash(passwordHash);
        setFullName(fullName);
        setRole(role);
        this.department = department;
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
     * Asigna el identificador único del usuario.
     *
     * @param id Identificador primario a asignar.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Correo electrónico registrado.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Asigna el correo electrónico del usuario validando longitud máxima.
     *
     * @param email Correo electrónico a asignar.
     * @throws IllegalArgumentException si el correo excede los 150 caracteres.
     */
    public void setEmail(String email) {
        if (email != null && email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("El correo electrónico no puede exceder los " + MAX_EMAIL_LENGTH + " caracteres");
        }
        this.email = email;
    }

    /**
     * Obtiene el hash BCrypt de la contraseña del usuario.
     *
     * @return Hash seguro de la contraseña.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Asigna el hash seguro de la contraseña validando longitud máxima.
     *
     * @param passwordHash Hash a asignar.
     * @throws IllegalArgumentException si el hash excede los 255 caracteres.
     */
    public void setPasswordHash(String passwordHash) {
        if (passwordHash != null && passwordHash.length() > MAX_PASSWORD_HASH_LENGTH) {
            throw new IllegalArgumentException("El hash de contraseña no puede exceder los " + MAX_PASSWORD_HASH_LENGTH + " caracteres");
        }
        this.passwordHash = passwordHash;
    }

    /**
     * Obtiene el nombre completo del usuario.
     *
     * @return Nombre completo y apellidos.
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Asigna el nombre completo del usuario validando longitud máxima.
     *
     * @param fullName Nombre completo a asignar.
     * @throws IllegalArgumentException si el nombre excede los 150 caracteres.
     */
    public void setFullName(String fullName) {
        if (fullName != null && fullName.length() > MAX_FULL_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre completo no puede exceder los " + MAX_FULL_NAME_LENGTH + " caracteres");
        }
        this.fullName = fullName;
    }

    /**
     * Obtiene el rol funcional del usuario.
     *
     * @return Rol actual asignado.
     */
    public RolUsuario getRole() {
        return role;
    }

    /**
     * Asigna el rol funcional del usuario, aplicando {@link RolUsuario#CITIZEN} si se proporciona null.
     *
     * @param role Rol a asignar.
     */
    public void setRole(RolUsuario role) {
        this.role = (role != null) ? role : RolUsuario.CITIZEN;
    }

    /**
     * Obtiene el departamento municipal asignado al usuario.
     *
     * @return Entidad {@link Departamento} o {@code null} si no tiene ninguno asignado.
     */
    public Departamento getDepartment() {
        return department;
    }

    /**
     * Asigna el departamento municipal al usuario.
     *
     * @param department Entidad del departamento a vincular.
     */
    public void setDepartment(Departamento department) {
        this.department = department;
    }

    /**
     * Método de conveniencia para obtener el identificador numérico del departamento asignado.
     *
     * @return Identificador del departamento o {@code null} si no tiene departamento vinculado.
     */
    public Long getDepartmentId() {
        return department != null ? department.getId() : null;
    }

    /**
     * Obtiene la fecha y hora de creación del registro.
     *
     * @return Marca de tiempo en UTC con zona horaria como {@link OffsetDateTime}.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Asigna la fecha y hora de creación del registro.
     *
     * @param createdAt Marca de tiempo a asignar.
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Representación textual de la entidad que omite deliberadamente {@code passwordHash}
     * para preservar la confidencialidad y prevenir fugas de seguridad en logs.
     *
     * @return Cadena informativa con id, email, fullName, role, departmentId y createdAt.
     */
    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", departmentId=" + getDepartmentId() +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Evalúa la igualdad entre dos instancias de {@link Usuario}.
     * Si ambos disponen de identificador primario persistido, se comparan por {@code id};
     * en caso contrario se recurre a la unicidad del {@code email}.
     *
     * @param o Objeto a comparar.
     * @return {@code true} si se consideran equivalentes, {@code false} en caso contrario.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        if (id != null && usuario.id != null) {
            return Objects.equals(id, usuario.id);
        }
        return Objects.equals(email, usuario.email);
    }

    /**
     * Genera el código hash de la instancia.
     * Utiliza {@code id} si está presente, o {@code email} en caso contrario.
     *
     * @return Código hash entero.
     */
    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(email);
    }
}
