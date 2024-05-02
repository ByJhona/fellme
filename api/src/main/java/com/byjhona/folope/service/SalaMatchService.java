package com.byjhona.folope.service;

import com.byjhona.folope.domain.filme.FilmeDescobertaDTO;
import com.byjhona.folope.domain.relac_sala_match_filme_curtido.RelacSalaMatchFilmeCurtido;
import com.byjhona.folope.domain.relac_sala_match_filme_curtido.RelacSalaMatchFilmeCurtidoDTO;
import com.byjhona.folope.domain.relac_sala_match_filmes.RelacSalaMatchFilmes;
import com.byjhona.folope.domain.relac_sala_match_filmes.RelacSalaMatchFilmesDTO;
import com.byjhona.folope.domain.sala_match.SalaMatch;
import com.byjhona.folope.domain.sala_match.SalaMatchDTO;
import com.byjhona.folope.exception.NaoAutorizadoException;
import com.byjhona.folope.repository.*;
import com.byjhona.folope.types.StatusSolicitacao;
import com.byjhona.folope.util.TratadorParametros;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalaMatchService {
    @Autowired
    private SalaMatchRepository salaMatchRepo;
    @Autowired
    private RelacSalaMatchFilmesRepository salaMatchFilmesRepo;
    @Autowired
    private TmdbAPI tmdbAPI;
    @Autowired
    private RelacUsuarioGeneroCurtidoRepository usuarioGeneroRepo;
    @Autowired
    private RelacUsuarioFilmeCurtidoRepository usuarioFilmeRepo;
    @Autowired
    private RelacSalaMatchFilmeCurtidoRepository salaMatchFilmeCurtidoRepo;

    @Transactional
    public SalaMatchDTO cadastrar(SalaMatch salaMatch) {
        System.out.println(salaMatch.getStatus());
        salaMatchRepo.save(salaMatch);
        escolherFilmesSala(salaMatch);
        return new SalaMatchDTO(salaMatch);
    }

    @Transactional
    public void escolherFilmesSala(SalaMatch salaMatch) {
        Long idSalaMatch = salaMatch.getId();
        Long idAnfitriao = salaMatch.getIdAnfitriao();
        Long idHospede = salaMatch.getIdHospede();
        String generos = generoMatchParaParametro(idAnfitriao, idHospede);

        Long recomendacaoAnfitriao = usuarioFilmeRepo.sortearFilmeAleatorio(idAnfitriao);
        Long recomendacaoHospede = usuarioFilmeRepo.sortearFilmeAleatorio(idHospede);

        List<FilmeDescobertaDTO> filmesRecomedacaoAnfitriao;
        List<FilmeDescobertaDTO> filmesRecomedacaoHospede;
        List<FilmeDescobertaDTO> filmesDescoberta = tmdbAPI.buscarFilmesDescoberta(generos);
        List<FilmeDescobertaDTO> filmesSala = new ArrayList<FilmeDescobertaDTO>();

        filmesSala.addAll(filmesDescoberta);

        if (recomendacaoAnfitriao != null) {
            filmesRecomedacaoAnfitriao = tmdbAPI.buscarFilmesRecomendacao(recomendacaoAnfitriao);
            filmesSala.addAll(filmesRecomedacaoAnfitriao);
        }
        if (recomendacaoHospede != null) {
            filmesRecomedacaoHospede = tmdbAPI.buscarFilmesRecomendacao(recomendacaoHospede);
            filmesSala.addAll(filmesRecomedacaoHospede);
        }

        List<FilmeDescobertaDTO> filmesTratadosSala = tratarListaFilmes(filmesSala);
        salvarFilmesNoBanco(filmesTratadosSala, idSalaMatch);
    }

    private String generoMatchParaParametro(Long idAnfitriao, Long idHospede) {
        List<Long> generosAnfitriao = usuarioGeneroRepo.generosCurtidos(idAnfitriao);
        List<Long> generosHospede = usuarioGeneroRepo.generosCurtidos(idHospede);
        List<Long> generosMatch = new ArrayList<>();

        for (Long item : generosAnfitriao) {
            if (generosHospede.contains(item)) {
                generosMatch.add(item);
            }
        }
        String genero = generosMatch.stream().map(String::valueOf).collect(Collectors.joining(","));
        genero = TratadorParametros.tratar(null, genero, null);
        return genero;
    }

    private List<FilmeDescobertaDTO> tratarListaFilmes(List<FilmeDescobertaDTO> filmesLista) {
        int limiarFilmes = 80;
        int maxTentativas = 5;
        int maxFilmesRetornados = 50;
        int quantFilmes = filmesLista.size();
        List<FilmeDescobertaDTO> filmesTratados = filmesLista.stream().distinct().collect(Collectors.toList());
        while (quantFilmes < limiarFilmes && maxTentativas > 0) {
            List<FilmeDescobertaDTO> filmesExtras = tmdbAPI.buscarFilmesDescoberta();
            filmesExtras = filmesExtras.stream().distinct().toList();
            filmesTratados.addAll(filmesExtras);
            quantFilmes = filmesTratados.size();
            maxTentativas--;
        }

        Collections.shuffle(filmesTratados);
        return filmesTratados.subList(0, maxFilmesRetornados);
    }

    private void salvarFilmesNoBanco(List<FilmeDescobertaDTO> filmes, Long idSalaMatch) {
        for (FilmeDescobertaDTO t : filmes) {
            Long idFilme = t.id();
            RelacSalaMatchFilmes relacSalaMatchFilmes = new RelacSalaMatchFilmes(idSalaMatch, idFilme);
            salaMatchFilmesRepo.save(relacSalaMatchFilmes);
        }
    }

    public SalaMatchDTO mostrar(Long id) {
        SalaMatch salaMatch = salaMatchRepo.getReferenceById(id);
        return new SalaMatchDTO(salaMatch);
    }

    public List<RelacSalaMatchFilmesDTO> listarFilmesSalaMatch(Long id) {
        List<RelacSalaMatchFilmes> salaFilmesMatch = salaMatchFilmesRepo.listarFilmesMatch(id);

        List<RelacSalaMatchFilmesDTO> salaFilmesMatchDTO = new ArrayList<>();
        for (RelacSalaMatchFilmes dados : salaFilmesMatch) {
            RelacSalaMatchFilmesDTO salaFilmeMatch = new RelacSalaMatchFilmesDTO(dados.getIdSalaMatch(), dados.getIdFilme());
            salaFilmesMatchDTO.add(salaFilmeMatch);
        }
        return salaFilmesMatchDTO;
    }

    public List<RelacSalaMatchFilmeCurtidoDTO> mostrarFilmesCurtidos(Long id) {
        List<RelacSalaMatchFilmeCurtido> salaFilmesMatch = salaMatchFilmeCurtidoRepo.listarFilmesMatch(id);
        List<RelacSalaMatchFilmeCurtidoDTO> salaMatchFilmeCurtidoDTO = new ArrayList<>();
        for (RelacSalaMatchFilmeCurtido dados : salaFilmesMatch) {
            RelacSalaMatchFilmeCurtidoDTO salaFilmeMatch = new RelacSalaMatchFilmeCurtidoDTO(dados.getIdSalaMatch(), dados.getIdUsuario(), dados.getIdFilme());
            salaMatchFilmeCurtidoDTO.add(salaFilmeMatch);
        }
        return salaMatchFilmeCurtidoDTO;
    }

    public RelacSalaMatchFilmeCurtidoDTO cadastrarFilmeCurtido(RelacSalaMatchFilmeCurtido filmeCurtido) {
        RelacSalaMatchFilmeCurtidoDTO filmeCurtidoDTO = new RelacSalaMatchFilmeCurtidoDTO(filmeCurtido.getIdSalaMatch(), filmeCurtido.getIdUsuario(), filmeCurtido.getIdFilme());
        salaMatchFilmeCurtidoRepo.save(filmeCurtido);
        return filmeCurtidoDTO;
    }

    public void responderConviteMatch(String resposta, Long idSala, Long idUsuario) {
        SalaMatch salaMatch = salaMatchRepo.getReferenceById(idSala);

        if (!salaMatch.getIdHospede().equals(idUsuario)) {
            throw new NaoAutorizadoException();
        }

        if (resposta.equals(StatusSolicitacao.Aceito.toString()) || resposta.equals(StatusSolicitacao.Recusado.toString())) {
            salaMatch.setStatus(resposta);
            salaMatchRepo.save(salaMatch);
        }
    }


    public void deletar(Long id) {
        salaMatchRepo.deleteById(id);
    }
}
