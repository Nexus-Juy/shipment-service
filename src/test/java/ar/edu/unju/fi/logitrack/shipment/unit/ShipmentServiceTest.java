package ar.edu.unju.fi.logitrack.shipment.unit;

import ar.edu.unju.fi.logitrack.shipment.dto.envio.CambiarEstadoRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.DatosClienteRequest;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.EnvioResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.DireccionResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.HistorialEstadoResponse;
import ar.edu.unju.fi.logitrack.shipment.dto.envio.RegistrarEnvioRequest;
import ar.edu.unju.fi.logitrack.shipment.entity.*;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import ar.edu.unju.fi.logitrack.shipment.exception.BusinessRuleException;
import ar.edu.unju.fi.logitrack.shipment.exception.InvalidStateTransitionException;
import ar.edu.unju.fi.logitrack.shipment.exception.ResourceNotFoundException;
import ar.edu.unju.fi.logitrack.shipment.mapper.EnvioMapper;
import ar.edu.unju.fi.logitrack.shipment.repository.*;
import ar.edu.unju.fi.logitrack.shipment.service.impl.ShipmentServiceImpl;
import ar.edu.unju.fi.logitrack.shipment.service.TrackingCodeGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la clase {@link ShipmentServiceImpl}.
 * <p>
 * Esta clase valida las reglas de negocio del servicio de envíos aislando
 * la capa de persistencia y mapeo mediante dobles de prueba (Mocks).
 * </p>
 */
@ExtendWith(MockitoExtension.class) // Habilita la integración del framework Mockito con JUnit 5
class ShipmentServiceTest {

    // =========================================================================
    // DEPENDENCIAS MOCKEADAS (MOCKS)
    // =========================================================================

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private HistorialEstadoRepository historialEstadoRepository;

    @Mock
    private EnvioMapper envioMapper;

    /**
     * Generador de códigos de seguimiento real. Se usa como espía para que arme códigos de verdad
     * y a la vez poder revisar cuántas veces lo llamó el servicio.
     */
    @Spy
    private TrackingCodeGenerator trackingCodeGenerator = new TrackingCodeGenerator();

    // =========================================================================
    // INYECCIÓN DEL SERVICIO BAJO PRUEBA
    // =========================================================================

    /**
     * Inyecta automáticamente los Mocks declarados arriba dentro de la
     * implementación del servicio ShipmentServiceImpl.
     */
    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    // Fixtures o datos de prueba compartidos
    private Cliente clienteEjemplo;
    private Envio envioEjemplo;

    /**
     * Configuración previa a la ejecución de cada prueba unitaria (@Test).
     * Prepara las entidades básicas con datos válidos para evitar duplicidad de código.
     */
    @BeforeEach
    void setUp() {
        // Inicialización de un cliente simulado
        clienteEjemplo = new Cliente("Juan Perez", "juan@gmail.com", "3881234567");

        // Creación de las direcciones de origen y destino
        Direccion origen = new Direccion("Calle 1", "123", "Jujuy", "4600");
        Direccion destino = new Direccion("Calle 2", "456", "Jujuy", "4600");

        // Creación del envío de prueba utilizando el constructor de dominio
        envioEjemplo = new Envio(
            "TRK-12345678",
            clienteEjemplo,
            origen,
            destino,
            new BigDecimal("2.500"),
            new BigDecimal("10.00"),
            new BigDecimal("10.00"),
            new BigDecimal("10.00")
        );
    }

    // =========================================================================
    // CASOS DE PRUEBA UNITARIOS
    // =========================================================================

    /**
     * Verifica que un envío se registre correctamente cuando el cliente existe
     * y los datos recibidos en el DTO son válidos.
     */
    @Test
    @DisplayName("Registrar envío exitosamente")
    void testRegistrarEnvioExitoso() {
        // GIVEN (Dado): Configuración del escenario de prueba
        UUID clienteId = UUID.randomUUID();
        RegistrarEnvioRequest request = new RegistrarEnvioRequest(
            clienteId, "Calle 1", "123", "Jujuy", "4600",
            "Calle 2", "456", "Jujuy", "4600",
            new BigDecimal("2.5"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
            null
        );

        // Definición de comportamientos esperados de los Mocks (Stubbing)
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteEjemplo));
        when(envioRepository.save(any(Envio.class))).thenReturn(envioEjemplo);
        when(envioMapper.toResponse(any(Envio.class))).thenReturn(new EnvioResponse(
            UUID.randomUUID(), "TRK-12345678", EstadoEnvio.PENDIENTE, clienteId, null,
            new DireccionResponse("Calle 1", "123", "Jujuy", "4600"),
            new DireccionResponse("Calle 2", "456", "Jujuy", "4600"),
            new BigDecimal("2.5"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
            new BigDecimal("2625.00"), Instant.now()
        ));

        // WHEN (Cuando): Ejecución del método bajo prueba
        EnvioResponse response = shipmentService.registrarEnvio(request);

        // THEN (Entonces): Verificación de los resultados esperados (Aserciones)
        assertNotNull(response, "La respuesta no debe ser nula");
        assertEquals(EstadoEnvio.PENDIENTE, response.estado(), "El estado inicial del envío debe ser PENDIENTE");

        // Verificación de llamadas a las dependencias
        verify(envioRepository, times(1)).save(any(Envio.class));
    }

    /**
     * Verifica que al cancelar un envío en estado PENDIENTE, su estado cambie
     * exitosamente a CANCELADO y se persistan los cambios en la base de datos.
     */
    @Test
    @DisplayName("Cancelar envío en estado PENDIENTE modifica su estado correctamente")
    void testCancelarEnvioExitoso() {
        // GIVEN (Dado): El envío existe en la BD y está en estado PENDIENTE
        UUID envioId = UUID.randomUUID();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));
        when(envioRepository.save(any(Envio.class))).thenReturn(envioEjemplo);

        // WHEN (Cuando): Se solicita la cancelación del envío
        shipmentService.cancelarEnvio(envioId);

        // THEN (Entonces): Se valida que la entidad cambió su estado interno a CANCELADO
        assertEquals(EstadoEnvio.CANCELADO, envioEjemplo.getEstado(), "El estado debería haber cambiado a CANCELADO");

        // Se confirma que se llamó al método save del repositorio
        verify(envioRepository, times(1)).save(envioEjemplo);
    }

    /**
     * Verifica que se lance una excepción de regla de negocio (BusinessRuleException)
     * al intentar cancelar un envío que ya no está en estado PENDIENTE (por ejemplo, EN_RUTA).
     */
    @Test
    @DisplayName("Lanzar BusinessRuleException al intentar cancelar un envío que no está en PENDIENTE")
    void testCancelarEnvioInvalidoLanzaExcepcion() {
        // GIVEN (Dado): Un envío que ya posee un repartidor asignado y se encuentra EN_RUTA
        UUID envioId = UUID.randomUUID();

        // Transiciones del modelo de dominio
        envioEjemplo.asignarRepartidor(UUID.randomUUID());
        envioEjemplo.cambiarEstado(EstadoEnvio.EN_RUTA);

        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));

        // WHEN & THEN (Cuando y Entonces): Se espera que la invocación lance la excepción
        assertThrows(BusinessRuleException.class,
            () -> shipmentService.cancelarEnvio(envioId),
            "Debe lanzar BusinessRuleException al intentar cancelar un envío EN_RUTA");

        // Se verifica que NUNCA se haya guardado un cambio en el repositorio
        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Arma un pedido para registrar un envío con el peso y las medidas que se le pasan.
     * Las direcciones son siempre las mismas porque no influyen en la tarifa.
     *
     * @param clienteId id del cliente que manda el paquete
     * @param peso peso real del paquete en kilos
     * @param alto alto del paquete en centímetros
     * @param ancho ancho del paquete en centímetros
     * @param largo largo del paquete en centímetros
     * @return el pedido listo para mandarle al servicio
     */
    private RegistrarEnvioRequest armarPedido(UUID clienteId, double peso, double alto, double ancho, double largo) {
        return new RegistrarEnvioRequest(
            clienteId, "Calle 1", "123", "Jujuy", "4600",
            "Calle 2", "456", "Jujuy", "4600",
            BigDecimal.valueOf(peso), BigDecimal.valueOf(ancho), BigDecimal.valueOf(alto), BigDecimal.valueOf(largo),
            null
        );
    }

    /**
     * Registra un envío usando el servicio y devuelve el envío que el servicio mandó a guardar.
     * Lo usamos en los tests de tarifa para poder revisar cuánto se le cobró al paquete.
     *
     * @param peso peso real del paquete en kilos
     * @param alto alto del paquete en centímetros
     * @param ancho ancho del paquete en centímetros
     * @param largo largo del paquete en centímetros
     * @return el envío que llegó al repositorio
     */
    private Envio registrarYObtenerEnvioGuardado(double peso, double alto, double ancho, double largo) {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(clienteEjemplo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.registrarEnvio(armarPedido(clienteId, peso, alto, ancho, largo));

        ArgumentCaptor<Envio> envioGuardado = ArgumentCaptor.forClass(Envio.class);
        verify(envioRepository).save(envioGuardado.capture());
        return envioGuardado.getValue();
    }

    /**
     * Deja el envío de ejemplo en estado EN_RUTA, asignándole antes un repartidor activo
     * porque sin repartidor no puede salir a la calle.
     */
    private void ponerEnvioEnRuta() {
        envioEjemplo.asignarRepartidor(UUID.randomUUID());
        envioEjemplo.cambiarEstado(EstadoEnvio.EN_RUTA);
    }

    /**
     * Si el paquete pesa más de lo que ocupa, se cobra por el peso real.
     * Peso volumétrico: 10 x 10 x 10 / 5000 = 0,2 kg, entonces se cobran los 10 kg reales.
     * Tarifa esperada: 1500 + 450 x 10 = 6000.
     */
    @Test
    @DisplayName("Tarifa: se cobra por el peso real cuando es mayor al peso volumétrico")
    void testTarifaUsaPesoReal() {
        Envio envio = registrarYObtenerEnvioGuardado(10.0, 10.0, 10.0, 10.0);

        assertEquals(new BigDecimal("6000.00"), envio.getTarifa());
    }

    /**
     * Si el paquete es grande pero liviano, se cobra por lo que ocupa.
     * Peso volumétrico: 50 x 40 x 30 / 5000 = 12 kg, que es más que el kilo real.
     * Tarifa esperada: 1500 + 450 x 12 = 6900.
     */
    @Test
    @DisplayName("Tarifa: se cobra por el peso volumétrico cuando es mayor al peso real")
    void testTarifaUsaPesoVolumetrico() {
        Envio envio = registrarYObtenerEnvioGuardado(1.0, 50.0, 40.0, 30.0);

        assertEquals(new BigDecimal("6900.00"), envio.getTarifa());
    }

    /**
     * Revisa que la tarifa siempre quede con 2 decimales, como un precio normal.
     * Tarifa esperada: 1500 + 450 x 1,333 = 2099,85.
     */
    @Test
    @DisplayName("Tarifa: el resultado se redondea a 2 decimales")
    void testTarifaRedondeaADosDecimales() {
        Envio envio = registrarYObtenerEnvioGuardado(1.333, 1.0, 1.0, 1.0);

        assertEquals(new BigDecimal("2099.85"), envio.getTarifa());
        assertEquals(2, envio.getTarifa().scale());
    }

    /**
     * Un envío PENDIENTE que ya tiene repartidor puede pasar a EN_RUTA, y el cambio se guarda.
     */
    @Test
    @DisplayName("Estados: un envío PENDIENTE con repartidor pasa a EN_RUTA")
    void testCambiarEstadoDePendienteAEnRuta() {
        UUID envioId = UUID.randomUUID();
        envioEjemplo.asignarRepartidor(UUID.randomUUID());
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA));

        assertEquals(EstadoEnvio.EN_RUTA, envioEjemplo.getEstado());
        verify(envioRepository, times(1)).save(envioEjemplo);
    }

    /**
     * Un envío que está EN_RUTA puede pasar a ENTREGADO.
     */
    @Test
    @DisplayName("Estados: un envío EN_RUTA pasa a ENTREGADO")
    void testCambiarEstadoDeEnRutaAEntregado() {
        UUID envioId = UUID.randomUUID();
        ponerEnvioEnRuta();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.ENTREGADO));

        assertEquals(EstadoEnvio.ENTREGADO, envioEjemplo.getEstado());
        verify(envioRepository, times(1)).save(envioEjemplo);
    }

    /**
     * No se puede saltar de PENDIENTE a ENTREGADO sin pasar por EN_RUTA.
     * El envío tiene que quedar como estaba y no se tiene que guardar nada.
     */
    @Test
    @DisplayName("Estados: no se puede pasar de PENDIENTE a ENTREGADO")
    void testNoSePuedeSaltearEnRuta() {
        UUID envioId = UUID.randomUUID();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));

        assertThrows(InvalidStateTransitionException.class,
            () -> shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.ENTREGADO)));

        assertEquals(EstadoEnvio.PENDIENTE, envioEjemplo.getEstado());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Un envío que ya se entregó no puede volver a salir a la calle.
     */
    @Test
    @DisplayName("Estados: un envío ENTREGADO no puede volver a EN_RUTA")
    void testEnvioEntregadoNoVuelveAtras() {
        UUID envioId = UUID.randomUUID();
        ponerEnvioEnRuta();
        envioEjemplo.cambiarEstado(EstadoEnvio.ENTREGADO);
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));

        assertThrows(InvalidStateTransitionException.class,
            () -> shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA)));

        assertEquals(EstadoEnvio.ENTREGADO, envioEjemplo.getEstado());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Un envío cancelado ya terminó su recorrido, así que no puede cambiar a ningún otro estado.
     */
    @Test
    @DisplayName("Estados: un envío CANCELADO no puede pasar a EN_RUTA")
    void testEnvioCanceladoNoCambiaDeEstado() {
        UUID envioId = UUID.randomUUID();
        envioEjemplo.cancelar();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));

        assertThrows(InvalidStateTransitionException.class,
            () -> shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA)));

        assertEquals(EstadoEnvio.CANCELADO, envioEjemplo.getEstado());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Aunque PENDIENTE a EN_RUTA es un cambio permitido, el envío no puede salir si nadie lo lleva.
     */
    @Test
    @DisplayName("Estados: no se puede pasar a EN_RUTA sin repartidor asignado")
    void testNoSalirEnRutaSinRepartidor() {
        UUID envioId = UUID.randomUUID();
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
            () -> shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA)));

        assertTrue(error.getMessage().contains("repartidor"));
        assertEquals(EstadoEnvio.PENDIENTE, envioEjemplo.getEstado());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Si se pide cambiar el estado de un envío que no existe, el servicio avisa que no lo encontró.
     */
    @Test
    @DisplayName("Estados: cambiar el estado de un envío que no existe lanza ResourceNotFoundException")
    void testCambiarEstadoDeEnvioInexistente() {
        UUID envioId = UUID.randomUUID();
        when(envioRepository.findById(envioId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA)));

        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * Cada vez que el envío cambia de estado se tiene que agregar un registro nuevo al historial,
     * guardando de qué estado venía y a cuál pasó.
     */
    @Test
    @DisplayName("Historial: cada cambio de estado agrega un registro de auditoría")
    void testCadaCambioDeEstadoQuedaEnElHistorial() {
        UUID envioId = UUID.randomUUID();
        envioEjemplo.asignarRepartidor(UUID.randomUUID());
        when(envioRepository.findById(envioId)).thenReturn(Optional.of(envioEjemplo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.EN_RUTA));
        shipmentService.cambiarEstado(envioId, new CambiarEstadoRequest(EstadoEnvio.ENTREGADO));

        List<HistorialEstado> historial = envioEjemplo.getHistorial();
        assertEquals(3, historial.size());
        HistorialEstado ultimoCambio = historial.get(2);
        assertEquals(EstadoEnvio.EN_RUTA, ultimoCambio.getEstadoAnterior());
        assertEquals(EstadoEnvio.ENTREGADO, ultimoCambio.getEstadoNuevo());
        assertNotNull(ultimoCambio.getFechaHora());
    }

    /**
     * La consulta del historial devuelve todos los registros del envío, empezando por el más nuevo.
     */
    @Test
    @DisplayName("Historial: se devuelven todos los cambios del envío, del más nuevo al más viejo")
    void testObtenerHistorialDeUnEnvio() {
        UUID envioId = UUID.randomUUID();
        ponerEnvioEnRuta();
        envioEjemplo.cambiarEstado(EstadoEnvio.ENTREGADO);
        List<HistorialEstado> registros = new ArrayList<>(envioEjemplo.getHistorial());
        Collections.reverse(registros);

        when(envioRepository.existsById(envioId)).thenReturn(true);
        when(historialEstadoRepository.findByEnvioIdOrderByFechaHoraDesc(envioId)).thenReturn(registros);
        when(envioMapper.toHistorialResponse(any(HistorialEstado.class))).thenAnswer(invocacion -> {
            HistorialEstado registro = invocacion.getArgument(0);
            return new HistorialEstadoResponse(registro.getEstadoAnterior(), registro.getEstadoNuevo(), registro.getFechaHora());
        });

        List<HistorialEstadoResponse> historial = shipmentService.obtenerHistorial(envioId);

        assertEquals(3, historial.size());
        assertEquals(EstadoEnvio.ENTREGADO, historial.get(0).estadoNuevo());
        assertEquals(EstadoEnvio.PENDIENTE, historial.get(2).estadoNuevo());
        assertNull(historial.get(2).estadoAnterior());
    }

    /**
     * Si se pide el historial de un envío que no existe, el servicio avisa que no lo encontró
     * y ni siquiera llega a buscar el historial.
     */
    @Test
    @DisplayName("Historial: pedir el historial de un envío que no existe lanza ResourceNotFoundException")
    void testObtenerHistorialDeEnvioInexistente() {
        UUID envioId = UUID.randomUUID();
        when(envioRepository.existsById(envioId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> shipmentService.obtenerHistorial(envioId));

        verify(historialEstadoRepository, never()).findByEnvioIdOrderByFechaHoraDesc(any(UUID.class));
    }

    /**
     * Arma un pedido de registro que no trae el clienteId, sino los datos del cliente.
     *
     * @param datos datos del cliente que se mandan en el pedido
     * @return el pedido listo para mandarle al servicio
     */
    private RegistrarEnvioRequest armarPedidoConDatosDeCliente(DatosClienteRequest datos) {
        return new RegistrarEnvioRequest(
            null, "Calle 1", "123", "Jujuy", "4600",
            "Calle 2", "456", "Jujuy", "4600",
            new BigDecimal("2.5"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
            datos
        );
    }

    /**
     * Si se mandan los datos de un cliente que no existe, el servicio lo registra
     * y el envío queda a nombre de ese cliente nuevo.
     */
    @Test
    @DisplayName("Cliente: con los datos de un cliente nuevo se registra el cliente y el envío")
    void testRegistrarEnvioConClienteNuevo() {
        DatosClienteRequest datos = new DatosClienteRequest("Ana Torres", "ana@gmail.com", "3884000000");
        when(clienteRepository.findByEmailIgnoreCase("ana@gmail.com")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.registrarEnvio(armarPedidoConDatosDeCliente(datos));

        ArgumentCaptor<Envio> envioGuardado = ArgumentCaptor.forClass(Envio.class);
        verify(envioRepository).save(envioGuardado.capture());
        assertEquals("ana@gmail.com", envioGuardado.getValue().getCliente().getEmail());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }

    /**
     * Si se mandan los datos de un cliente que ya estaba registrado (mismo email),
     * se usa ese cliente y no se crea otro.
     */
    @Test
    @DisplayName("Cliente: si el email ya existe se usa ese cliente y no se duplica")
    void testRegistrarEnvioReutilizaClienteExistente() {
        DatosClienteRequest datos = new DatosClienteRequest("Juan Perez", "juan@gmail.com", "3881234567");
        when(clienteRepository.findByEmailIgnoreCase("juan@gmail.com")).thenReturn(Optional.of(clienteEjemplo));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        shipmentService.registrarEnvio(armarPedidoConDatosDeCliente(datos));

        ArgumentCaptor<Envio> envioGuardado = ArgumentCaptor.forClass(Envio.class);
        verify(envioRepository).save(envioGuardado.capture());
        assertSame(clienteEjemplo, envioGuardado.getValue().getCliente());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    /**
     * Si el pedido no trae ni el clienteId ni los datos del cliente, no se puede saber
     * de quién es el envío, así que no se registra.
     */
    @Test
    @DisplayName("Cliente: sin clienteId ni datos del cliente no se registra el envío")
    void testRegistrarEnvioSinClienteLanzaExcepcion() {
        RegistrarEnvioRequest pedido = armarPedidoConDatosDeCliente(null);

        assertThrows(BusinessRuleException.class, () -> shipmentService.registrarEnvio(pedido));

        verify(envioRepository, never()).save(any(Envio.class));
    }

    /**
     * El código de seguimiento lo arma el {@link TrackingCodeGenerator} y empieza con "LT-".
     */
    @Test
    @DisplayName("Código: el envío registrado recibe un código de seguimiento que empieza con LT-")
    void testCodigoDeSeguimientoUsaElGenerador() {
        Envio envio = registrarYObtenerEnvioGuardado(2.5, 10.0, 10.0, 10.0);

        assertTrue(envio.getCodigoSeguimiento().startsWith("LT-"));
        verify(trackingCodeGenerator, times(1)).generar();
    }

    /**
     * Si el primer código que sale ya lo tiene otro envío, el servicio pide otro
     * hasta encontrar uno libre.
     */
    @Test
    @DisplayName("Código: si el código ya existe se genera otro")
    void testCodigoDeSeguimientoNoSeRepite() {
        when(envioRepository.existsByCodigoSeguimiento(anyString())).thenReturn(true, false);

        registrarYObtenerEnvioGuardado(2.5, 10.0, 10.0, 10.0);

        verify(trackingCodeGenerator, times(2)).generar();
    }

    // =========================================================================
    // LISTADO CON FILTROS
    // =========================================================================

    @Test
    @DisplayName("Listar: sin filtros devuelve todos los envíos")
    void testListarSinFiltros() {
        when(envioRepository.findAll()).thenReturn(List.of(envioEjemplo));

        assertEquals(1, shipmentService.listar(null, null).size());

        verify(envioRepository, never()).findByEstadoOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Listar: filtra solo por estado")
    void testListarPorEstado() {
        when(envioRepository.findByEstadoOrderByCreatedAtDesc(EstadoEnvio.PENDIENTE)).thenReturn(List.of(envioEjemplo));

        assertEquals(1, shipmentService.listar(EstadoEnvio.PENDIENTE, null).size());

        verify(envioRepository, never()).findAll();
    }

    @Test
    @DisplayName("Listar: filtra solo por repartidor")
    void testListarPorRepartidor() {
        UUID repartidorId = UUID.randomUUID();
        when(envioRepository.findByRepartidorIdOrderByCreatedAtDesc(repartidorId)).thenReturn(List.of(envioEjemplo));

        assertEquals(1, shipmentService.listar(null, repartidorId).size());

        verify(envioRepository, never()).findAll();
    }

    @Test
    @DisplayName("Listar: combina repartidor y estado")
    void testListarPorRepartidorYEstado() {
        UUID repartidorId = UUID.randomUUID();
        when(envioRepository.findByRepartidorIdAndEstadoOrderByCreatedAtDesc(repartidorId, EstadoEnvio.EN_RUTA))
            .thenReturn(List.of(envioEjemplo, envioEjemplo));

        assertEquals(2, shipmentService.listar(EstadoEnvio.EN_RUTA, repartidorId).size());
    }
}
