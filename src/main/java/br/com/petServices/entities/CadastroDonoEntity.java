package br.com.petServices.entities;

import java.util.ArrayList;
import java.util.List;


import br.com.petServices.services.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name="tb_dono")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
public class CadastroDonoEntity {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany (mappedBy = "dono", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CadastroPetEntity> pets = new ArrayList<>();

    @Column (nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
