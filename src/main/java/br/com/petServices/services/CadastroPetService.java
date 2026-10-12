package br.com.petServices.services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.petServices.dtos.CadastroPetDTO;
import br.com.petServices.dtos.CadastroPetResponse;
import br.com.petServices.entities.CadastroDonoEntity;
import br.com.petServices.entities.CadastroPetEntity;
import br.com.petServices.exceptions.RecursoNaoEncontradoException;
import br.com.petServices.repositories.CadastroDonoRepository;
import br.com.petServices.repositories.CadastroPetRepository;
import br.com.petServices.services.enums.evento.PetEvent;
import br.com.petServices.services.enums.evento.PetEventProducer;
import br.com.petServices.services.enums.evento.TipoEvento;

@Service
public class CadastroPetService {

    private final CadastroPetRepository cadastroPetRepository;
    private final CadastroDonoRepository cadastroDonoRepository;
    private final PetEventProducer petEventProducer;

    public CadastroPetService(CadastroPetRepository cadastroPetRepository, CadastroDonoRepository cadastroDonoRepository,
            PetEventProducer petEventProducer) {
        this.cadastroPetRepository = cadastroPetRepository;
        this.cadastroDonoRepository = cadastroDonoRepository;
        this.petEventProducer = petEventProducer;
    }

    public List<CadastroPetResponse> buscarTodos(){
        return cadastroPetRepository.findAll().stream()
        .map(e -> new CadastroPetResponse(e.getId(), e.getNome(), e.getDataNascimento(), e.getRaca(), e.getEspecie(), e.getDono().getId())).toList();
    }

    public CadastroPetResponse buscarPorId(Integer id){
        var pet = cadastroPetRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pet não encontrado com o ID: " + id));
        return new CadastroPetResponse(pet.getId(), pet.getNome(), pet.getDataNascimento(), pet.getRaca(), pet.getEspecie(), pet.getDono().getId());
    }
//salvarPet

    public CadastroPetResponse salvarPet(CadastroPetDTO request){
        CadastroDonoEntity dono = cadastroDonoRepository.findById(request.donoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Dono não encontrado com o ID: " + request.donoId()));

        CadastroPetEntity pet = new CadastroPetEntity();
        pet.setNome(request.nome());
        pet.setRaca(request.raca());
        pet.setDataNascimento(request.dataNascimento());
        pet.setEspecie(request.especie());
        pet.setDono(dono);

       CadastroPetEntity salvo = cadastroPetRepository.save(pet);
        petEventProducer.publicar(PetEvent.of(TipoEvento.CRIADO,
        salvo.getId(), salvo.getNome(), salvo.getRaca(), salvo.getDataNascimento(),
        salvo.getEspecie(), dono.getId())); 
        return new CadastroPetResponse(salvo.getId(), salvo.getNome(), salvo.getDataNascimento(),
        salvo.getRaca(), salvo.getEspecie(), dono.getId());
    }

    //DeletarPet

    public void deletarPet(Integer id){
        CadastroPetEntity pet = cadastroPetRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pet nao encontrado"));
        cadastroPetRepository.delete(pet);
        petEventProducer.publicar(PetEvent.of(TipoEvento.REMOVIDO,
        pet.getId(), pet.getNome(), pet.getRaca(), pet.getDataNascimento(),
        pet.getEspecie(), pet.getDono().getId()));
    }

    //atualizar

    public CadastroPetResponse atualizarPet (@PathVariable Integer id, CadastroPetDTO request){
        CadastroPetEntity pet = cadastroPetRepository.findById(id)
        .orElseThrow(()-> new RecursoNaoEncontradoException("Pet nao encontrado"));
        pet.setNome(request.nome());
        pet.setRaca(request.raca());
        pet.setEspecie(request.especie());
        pet.setDataNascimento(request.dataNascimento());

        CadastroPetEntity salvo = cadastroPetRepository.save(pet);
    petEventProducer.publicar(PetEvent.of(TipoEvento.ATUALIZADO,
            salvo.getId(), salvo.getNome(), salvo.getRaca(), salvo.getDataNascimento(),
            salvo.getEspecie(), salvo.getDono().getId()));

    return new CadastroPetResponse(salvo.getId(), salvo.getNome(), salvo.getDataNascimento(),
            salvo.getRaca(), salvo.getEspecie(), salvo.getDono().getId());
    }
}
