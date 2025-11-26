package com.supera.acessos.solicitacao.service;

import com.supera.acessos.exceptions.ApiException;
import com.supera.acessos.modulo.entity.Modulo;
import com.supera.acessos.modulo.repository.ModuloRepository;
import com.supera.acessos.solicitacao.dto.CriarSolicitacaoDTO;
import com.supera.acessos.solicitacao.entity.SolicitacaoModulo;
import com.supera.acessos.solicitacao.entity.StatusSolicitacao;
import com.supera.acessos.solicitacao.repository.SolicitacaoModuloRepository;
import com.supera.acessos.usuario.entity.Usuario;
import com.supera.acessos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoModuloServiceTest {

    @Mock
    private SolicitacaoModuloRepository solicitacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ModuloRepository moduloRepository;

    @InjectMocks
    private SolicitacaoModuloService service;

    private Usuario usuario;
    private Modulo modulo;

    @BeforeEach
    void setup() {
        usuario = Usuario.builder()
                .id(1L)
                .nome("Marcelo")
                .email("marceloo.cfreitas@gmail.com")
                .build();

        modulo = Modulo.builder()
                .id(10L)
                .nome("Financeiro")
                .ativo(true)
                .prazoExpiracaoDias(10)
                .build();
    }

    @Test
    void deveCriarSolicitacaoComSucesso() {

        //dto enviado pelo cliente
        CriarSolicitacaoDTO dto = new CriarSolicitacaoDTO(modulo.getId());

        //mock das dependências
        when(moduloRepository.findById(modulo.getId()))
                .thenReturn(Optional.of(modulo));

        when(solicitacaoRepository.existsBySolicitanteAndModuloAndStatusIn(
                any(), any(), any()))
                .thenReturn(false);

        //simula salva no banco
        when(solicitacaoRepository.save(any(SolicitacaoModulo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        //execucao
        SolicitacaoModulo solicitacao = service.criarSolicitacao(usuario, dto);

        //asserts
        assertNotNull(solicitacao);
        assertEquals(usuario, solicitacao.getSolicitante());
        assertEquals(modulo, solicitacao.getModulo());
        assertEquals(StatusSolicitacao.ABERTA, solicitacao.getStatus());

        //verifica se chamou o repository
        verify(solicitacaoRepository, times(1)).save(any());
    }

    @Test
    void naoDeveCriarSolicitacaoQuandoModuloInativo() {

        //modulo está inativo
        modulo.setAtivo(false);

        CriarSolicitacaoDTO dto = new CriarSolicitacaoDTO(modulo.getId());

        //mock do modulo
        when(moduloRepository.findById(modulo.getId()))
                .thenReturn(Optional.of(modulo));

        //execução e verificacao
        ApiException ex = assertThrows(
                ApiException.class, // ou ApiException se você já trocou
                () -> service.criarSolicitacao(usuario, dto)
        );

        assertEquals("Módulo inativo", ex.getMessage());
    }

    @Test
    void naoDeveCriarSolicitacaoQuandoUsuarioJaTemAcesso() {

        //simula que o usuario já tem o módulo
        usuario.getModulosAtivos().add(modulo);

        CriarSolicitacaoDTO dto = new CriarSolicitacaoDTO(modulo.getId());

        when(moduloRepository.findById(modulo.getId()))
                .thenReturn(Optional.of(modulo));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> service.criarSolicitacao(usuario, dto)
        );

        assertEquals("Usuário já possui acesso ao módulo", ex.getMessage());
    }

    @Test
    void naoDeveCriarSolicitacaoQuandoExistePendente() {

        CriarSolicitacaoDTO dto = new CriarSolicitacaoDTO(modulo.getId());

        when(moduloRepository.findById(modulo.getId()))
                .thenReturn(Optional.of(modulo));

        //simula que existe solicitação pendente
        when(solicitacaoRepository.existsBySolicitanteAndModuloAndStatusIn(
                eq(usuario),
                eq(modulo),
                any()
        )).thenReturn(true);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> service.criarSolicitacao(usuario, dto)
        );

        assertEquals("Já existe solicitação pendente para este módulo", ex.getMessage());
    }

    @Test
    void devePassarDeGestorParaSegurancaQuandoModuloExigeSeguranca() {

        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.AGUARDANDO_GESTOR)
                .dataAbertura(LocalDateTime.now())
                .build();

        modulo.setExigeAprovacaoSeguranca(true);

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo resposta = service.aprovarSolicitacao(1L, usuario);

        assertEquals(StatusSolicitacao.AGUARDANDO_SEGURANCA, resposta.getStatus());
        assertNull(resposta.getDataAprovacao()); // ainda não aprovada
        assertTrue(usuario.getModulosAtivos().isEmpty()); // nenhum acesso concedido

        verify(solicitacaoRepository, times(1)).save(any());
    }

    @Test
    void deveAprovarQuandoEstaAguardandoSeguranca() {

        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.AGUARDANDO_SEGURANCA)
                .dataAbertura(LocalDateTime.now())
                .build();

        //modulo com expiração de 10 dias
        modulo.setExigeAprovacaoSeguranca(true);

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo resposta = service.aprovarSolicitacao(1L, usuario);

        assertEquals(StatusSolicitacao.APROVADA, resposta.getStatus());
        assertNotNull(resposta.getDataAprovacao());
        assertNotNull(resposta.getDataExpiracao());

        //usuário recebeu o módulo
        assertTrue(usuario.getModulosAtivos().contains(modulo));

        verify(solicitacaoRepository, times(1)).save(any());
    }

    @Test
    void deveReprovarSolicitacaoQuandoPendente() {

        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.AGUARDANDO_GESTOR)
                .dataAbertura(LocalDateTime.now())
                .build();

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String motivo = "Informações insuficientes";

        SolicitacaoModulo resposta =
                service.reprovarSolicitacao(1L, usuario, motivo);

        assertEquals(StatusSolicitacao.REPROVADA, resposta.getStatus());
        assertEquals(motivo, resposta.getMotivoRecusa());
        assertNotNull(resposta.getDataReprovacao());

        verify(solicitacaoRepository, times(1)).save(any());
    }

    @Test
    void naoDeveReprovarQuandoSolicitacaoJaAprovada() {

        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now())
                .build();

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> service.reprovarSolicitacao(1L, usuario, "motivo")
        );

        assertEquals("Solicitação não pode ser reprovada", ex.getMessage());

        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveExpirarSolicitacaoQuandoDataExpirada() {

        //solicitacao já vencida
        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now().minusDays(15))
                .dataExpiracao(LocalDateTime.now().minusDays(5)) // expirada
                .build();

        usuario.getModulosAtivos().add(modulo); // usuário tinha acesso

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo resposta = service.detalharSolicitacao(1L, usuario);

        assertEquals(StatusSolicitacao.EXPIRADA, resposta.getStatus());
        assertFalse(usuario.getModulosAtivos().contains(modulo)); // módulo removido
        assertNotNull(resposta.getDataExpiracao()); // expiração continua registrada

        verify(usuarioRepository, times(1)).save(usuario);
        verify(solicitacaoRepository, times(1)).save(solicitacao);
    }

    @Test
    void deveRenovarSolicitacaoQuandoExpirada() {

        //solicitacao antiga já expirada
        SolicitacaoModulo antiga = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now().minusDays(20))
                .dataExpiracao(LocalDateTime.now().minusDays(5)) // expirada
                .build();

        usuario.getModulosAtivos().remove(modulo); // já deveria ter expirado antes

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(antiga));

        //simula salvar nova solicitação
        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo nova = service.renovarSolicitacao(1L, usuario);

        assertNotNull(nova);
        assertEquals(usuario, nova.getSolicitante());
        assertEquals(modulo, nova.getModulo());

        //nova solicitação deve começar como ABERTA ou AGUARDANDO_GESTOR
        assertTrue(
                nova.getStatus() == StatusSolicitacao.ABERTA ||
                        nova.getStatus() == StatusSolicitacao.AGUARDANDO_GESTOR ||
                        nova.getStatus() == StatusSolicitacao.AGUARDANDO_SEGURANCA ||
                        nova.getStatus() == StatusSolicitacao.APROVADA
        );

        verify(solicitacaoRepository, times(1)).save(any());
    }

    @Test
    void naoDeveRenovarQuandoSolicitacaoNaoExpirada() {

        //solicitacao ainda válida não expirada
        SolicitacaoModulo antiga = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now())
                .dataExpiracao(LocalDateTime.now().plusDays(5)) // AINDA VALIDA
                .build();

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(antiga));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> service.renovarSolicitacao(1L, usuario)
        );

        assertEquals("Somente solicitações expiradas podem ser renovadas.", ex.getMessage());

        //nao deve salvar nada
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveCancelarSolicitacaoAprovadaComSucesso() {

        //solicitacao que pode ser cancelada
        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now())
                .dataAprovacao(LocalDateTime.now().minusDays(3))
                .dataExpiracao(LocalDateTime.now().plusDays(7))
                .build();

        //usuário atualmente tem o módulo
        usuario.getModulosAtivos().add(modulo);

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(usuarioRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo resposta = service.cancelarSolicitacao(1L, usuario);

        assertEquals(StatusSolicitacao.CANCELADA, resposta.getStatus());
        assertNotNull(resposta.getDataCancelamento());

        //usuário nao deve mais ter o módulo
        assertFalse(usuario.getModulosAtivos().contains(modulo));

        verify(usuarioRepository, times(1)).save(usuario);
        verify(solicitacaoRepository, times(1)).save(solicitacao);
    }

    @Test
    void naoDeveCancelarQuandoSolicitacaoEmEstadoInvalido() {

        //solicitacao reprovada
        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.REPROVADA)
                .dataAbertura(LocalDateTime.now())
                .dataReprovacao(LocalDateTime.now().minusDays(1))
                .motivoRecusa("faltou justificativa")
                .build();

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        ApiException ex = assertThrows(
                ApiException.class,
                () -> service.cancelarSolicitacao(1L, usuario)
        );

        assertEquals("Solicitação não pode ser cancelada.", ex.getMessage());

        //nao deve salvar nenhum dos dois
        verify(solicitacaoRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveDetalharESimultaneamenteExpirarSolicitacaoSeNecessario() {

        // Solicitação já expirada
        SolicitacaoModulo solicitacao = SolicitacaoModulo.builder()
                .id(1L)
                .solicitante(usuario)
                .modulo(modulo)
                .status(StatusSolicitacao.APROVADA)
                .dataAbertura(LocalDateTime.now().minusDays(20))
                .dataExpiracao(LocalDateTime.now().minusDays(2)) // expirada
                .build();

        // Usuário possuía acesso
        usuario.getModulosAtivos().add(modulo);

        when(solicitacaoRepository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(solicitacaoRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoModulo resposta = service.detalharSolicitacao(1L, usuario);

        // A solicitação deve ser marcada como expirada
        assertEquals(StatusSolicitacao.EXPIRADA, resposta.getStatus());

        // O usuário deve perder o módulo
        assertFalse(usuario.getModulosAtivos().contains(modulo));

        // O sistema deve ter salvo as alterações
        verify(solicitacaoRepository, times(1)).save(solicitacao);
        verify(usuarioRepository, times(1)).save(usuario);
    }
}