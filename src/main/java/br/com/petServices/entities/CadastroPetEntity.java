package br.com.petServices.entities;

import java.time.LocalDate;

import br.com.petServices.services.enums.Especie;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name="tb_pet")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor

public class CadastroPetEntity {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "dono_id", nullable = false)
   private CadastroDonoEntity dono;

   @Column(nullable = false)
   private String nome;

   private LocalDate dataNascimento;

   private String raca;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false)
   private Especie especie;
}

