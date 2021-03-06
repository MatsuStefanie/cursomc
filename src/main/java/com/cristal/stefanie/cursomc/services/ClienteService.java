package com.cristal.stefanie.cursomc.services;

import com.cristal.stefanie.cursomc.domain.Categoria;
import com.cristal.stefanie.cursomc.domain.Cidade;
import com.cristal.stefanie.cursomc.domain.Cliente;
import com.cristal.stefanie.cursomc.domain.Endereco;
import com.cristal.stefanie.cursomc.domain.enuns.Perfil;
import com.cristal.stefanie.cursomc.domain.enuns.TipoCliente;
import com.cristal.stefanie.cursomc.dto.ClienteDTO;
import com.cristal.stefanie.cursomc.dto.ClienteNewDTO;
import com.cristal.stefanie.cursomc.repositores.ClienteRepository;
import com.cristal.stefanie.cursomc.repositores.EnderecoRepository;
import com.cristal.stefanie.cursomc.security.UserSS;
import com.cristal.stefanie.cursomc.services.exceptions.AuthorizationException;
import com.cristal.stefanie.cursomc.services.exceptions.DataIntregrityException;
import com.cristal.stefanie.cursomc.services.exceptions.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository repo;
    @Autowired
    private EnderecoRepository repoEnd;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ImageService imageService;

    @Value("${img.prefix.client.profile}")
    private String nomeDoArquivo;

    @Value("${img.profile.size}")
    private Integer size;

    public Cliente find(Integer id) {
        UserSS userSS = UserService.authenticated();
        if (userSS == null || !userSS.hasRole(Perfil.ADMIN) && !id.equals((userSS.getId()))) {
            throw new AuthorizationException("Acesso negado");
        }


        Optional<Cliente> obj = repo.findById(id);
        return obj.orElseThrow(() -> new ObjectNotFoundException(
                "Objeto não encontrado! Id: " + id + ", Tipo: " + Cliente.class.getName()));
    }

    @Transactional
    public Cliente insert(Cliente obj) {
        obj.setId(null);
        obj = repo.save(obj);
        repoEnd.saveAll(obj.getEnderecos());
        return obj;
    }

    public Cliente update(Cliente obj) {
        Cliente newOBJ = find(obj.getId());
        updateData(newOBJ, obj);
        return repo.save(newOBJ);
    }

    public void delete(Integer id) {
        find(id);
        try {
            repo.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntregrityException("não é possivel excluir um cliente que possui outras informações atreladas");
        }
    }

    public List<Cliente> findAll() {
        return repo.findAll();
    }

    public Cliente findByEmail(String email){
        UserSS userSS = UserService.authenticated();
        if(userSS==null || !userSS.hasRole(Perfil.ADMIN) && !email.equals(userSS.getUsername())){
            throw new AuthorizationException("Acesso negado");
        }

        Cliente obj = repo.findByEmail(email);
        if(obj == null){
            throw new ObjectNotFoundException("Objeto não encontrado. Id: "+ userSS.getId()
            + ", Tipo: " + Cliente.class.getName());
        }
        return  obj;
    }

    public Page<Cliente> findPage(Integer page, Integer linesPerPage, String direction, String orderBy) {
        PageRequest pageRequest = PageRequest.of(page, linesPerPage, Sort.Direction.valueOf(direction), orderBy);
        return repo.findAll(pageRequest);
    }

    public Cliente fromDTO(ClienteDTO objDTO) {
        return new Cliente(objDTO.getId(), objDTO.getNome(), objDTO.getEmail(), null, null, null);
    }

    public Cliente fromDTO(ClienteNewDTO objDTO) {

        Cidade cidade = new Cidade(objDTO.getCidadeId(), null, null);
        Cliente cliente = new Cliente(null, objDTO.getNome(), objDTO.getEmail(), objDTO.getCpfOuCnpj(), TipoCliente.toEnum(objDTO.getTipo()), bCryptPasswordEncoder.encode(objDTO.getSenha()));
        Endereco endereco = new Endereco(null, objDTO.getLogradouro(), objDTO.getNumero(), objDTO.getComplemento(), objDTO.getBairro(), objDTO.getCep(), cliente, cidade);
        cliente.getEnderecos().add(endereco);
        cliente.getTelefones().add(objDTO.getTelefone1());
        if (objDTO.getTelefone2() != null) {
            cliente.getTelefones().add(objDTO.getTelefone2());
        }
        if (objDTO.getTelefone3() != null) {
            cliente.getTelefones().add(objDTO.getTelefone3());
        }
        return cliente;
    }


    private void updateData(Cliente newObj, Cliente obj) {
        newObj.setNome(obj.getNome());
        newObj.setEmail(obj.getEmail());
    }

    public URI uploadProfilePicture(MultipartFile multipartFile) {
        UserSS userSS = UserService.authenticated();
        if (userSS == null) {
            throw new AuthorizationException("Acesso negado");
        }
        BufferedImage jpgImage = imageService.getJpgImageFromFile(multipartFile);
        jpgImage = imageService.cropSquare(jpgImage);
        jpgImage = imageService.resize(jpgImage, size);

        String fileName = nomeDoArquivo + userSS.getId() + ".jpg";

        return s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
    }
}
