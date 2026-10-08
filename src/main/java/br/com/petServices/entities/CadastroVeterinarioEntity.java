package br.com.petServices.entities;
import br.com.petServices.services.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name="tb_veterinario")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
public class CadastroVeterinarioEntity {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nome;

    @Column(nullable = false, unique = true)
    private String crm;

    private String especialidade;
    
    @Column(unique = true, nullable = false)
    private String email;

    @Column (nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
}

