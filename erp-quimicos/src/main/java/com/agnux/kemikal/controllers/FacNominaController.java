/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.agnux.kemikal.controllers;
import com.agnux.cfd.v2.ArchivoInformeMensual;
import com.agnux.cfd.v2.Base64Coder;
import com.agnux.cfd.v2.BeanFacturador;
import com.agnux.cfdi.BeanFacturadorCfdi;
import com.agnux.cfdi.adendas.AdendaCliente;
import com.agnux.cfdi.timbre.BeanFacturadorCfdiTimbre;
import com.agnux.common.helpers.FileHelper;
import com.agnux.kemikal.interfacedaos.PrefacturasInterfaceDao;
import com.agnux.common.helpers.StringHelper;
import com.agnux.common.helpers.TimeHelper;
import com.agnux.common.obj.DataPost;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.FacturasInterfaceDao;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import com.agnux.kemikal.reportes.pdfCfd_CfdiTimbrado;
import com.agnux.kemikal.reportes.pdfCfd_CfdiTimbradoFormato2;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 *
 * @author Noe Martinez
 * gpmarsan@gmail.com
 * 17/febrero/2014
 * 
 */
@Controller
@SessionAttributes({"user"})
@RequestMapping("/facnomina/")
public class FacNominaController {
    ResourceProject resource = new ResourceProject();
    private static final Logger log  = Logger.getLogger(FacNominaController.class.getName());
    
    @Autowired
    @Qualifier("daoFacturas")
    private FacturasInterfaceDao facdao;
        
    @Autowired
    @Qualifier("daoGral")
    private GralInterfaceDao gralDao;
    
    @Autowired
    @Qualifier("daoHome")
    private HomeInterfaceDao HomeDao;
    
    public FacturasInterfaceDao getFacdao() {
        return facdao;
    }
    
    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }
    
    public GralInterfaceDao getGralDao() {
        return gralDao;
    }
    
    
    @RequestMapping(value="/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response, 
            @ModelAttribute("user") UserSessionData user
        )throws ServletException, IOException {
        
        log.log(Level.INFO, "Ejecutando starUp de {0}", FacNominaController.class.getName());
        LinkedHashMap<String,String> infoConstruccionTabla = new LinkedHashMap<String,String>();
        
        infoConstruccionTabla.put("id", "Acciones:70");
        infoConstruccionTabla.put("no_periodo", "No. Periodo:100");
        infoConstruccionTabla.put("periodo", "Periodo:320");
        infoConstruccionTabla.put("fecha_pago", "Fecha Pago:100");
        infoConstruccionTabla.put("tipo", "Tipo:120");
        infoConstruccionTabla.put("fecha_creacion","Fecha Creaci&oacute;n:110");
        
        ModelAndView x = new ModelAndView("facnomina/startup", "title", "N&oacute;mina");
        
        x = x.addObject("layoutheader", resource.getLayoutheader());
        x = x.addObject("layoutmenu", resource.getLayoutmenu());
        x = x.addObject("layoutfooter", resource.getLayoutfooter());
        x = x.addObject("grid", resource.generaGrid(infoConstruccionTabla));
        x = x.addObject("url", resource.getUrl(request));
        x = x.addObject("username", user.getUserName());
        x = x.addObject("empresa", user.getRazonSocialEmpresa());
        x = x.addObject("sucursal", user.getSucursal());
        
        String userId = String.valueOf(user.getUserId());
        
        //System.out.println("id_de_usuario: "+userId);
        
        String codificado = Base64Coder.encodeString(userId);
        
        //id de usuario codificado
        x = x.addObject("iu", codificado);
        
        return x;
    }
    
    
    
    @RequestMapping(value="/getAllNominas.json", method = RequestMethod.POST)
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getAllNominasJson(
           @RequestParam(value="orderby", required=true) String orderby,
           @RequestParam(value="desc", required=true) String desc,
           @RequestParam(value="items_por_pag", required=true) int items_por_pag,
           @RequestParam(value="pag_start", required=true) int pag_start,
           @RequestParam(value="display_pag", required=true) String display_pag,
           @RequestParam(value="input_json", required=true) String input_json,
           @RequestParam(value="cadena_busqueda", required=true) String cadena_busqueda,
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {


        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String,String> has_busqueda = StringHelper.convert2hash(StringHelper.ascii2string(cadena_busqueda));

        //Aplicativo de Nomina
        Integer app_selected = 173;

        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));

        //Variables para el buscador
        String no_periodo = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("no_periodo")))+"%";
        String titulo_periodo = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("titulo_periodo")))+"%";
        String tipo_periodo = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("tipo_periodo")))+"";
        String fecha_inicial = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_inicial")))+"";
        String fecha_final = ""+StringHelper.isNullString(String.valueOf(has_busqueda.get("fecha_final")))+"";
        
        String data_string = app_selected+"___"+id_usuario+"___"+no_periodo+"___"+titulo_periodo+"___"+tipo_periodo+"___"+fecha_inicial+"___"+fecha_final;
        
        //Obtiene total de registros en base de datos, con los parametros de busqueda
        int total_items = this.getGralDao().countAll(data_string);
        
        //Calcula el total de paginas
        int total_pags = resource.calculaTotalPag(total_items,items_por_pag);
        
        //Variables que necesita el datagrid, para no tener que hacer uno por cada aplicativo
        DataPost dataforpos = new DataPost(orderby, desc, items_por_pag, pag_start, display_pag, input_json, cadena_busqueda,total_items,total_pags, id_user_cod);

        int offset = resource.__get_inicio_offset(items_por_pag, pag_start);

        //Obtiene los registros para el grid, de acuerdo a los parametros de busqueda
        jsonretorno.put("Data", this.getFacdao().getFacNomina_PaginaGrid(data_string, offset, items_por_pag, orderby, desc));
        
        //Obtiene el hash para los datos que necesita el datagrid
        jsonretorno.put("DataForGrid", dataforpos.formaHashForPos(dataforpos));
        
        return jsonretorno;
    }

    
    
    
    //Obtiene los Tipos de Periodicidad para el Buscador
    @RequestMapping(method = RequestMethod.POST, value="/getDatosParaBuscador.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getDatosParaBuscadorJson(
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        jsonretorno.put("TiposPeriodicidadBusqueda", this.getFacdao().getFacNomina_PeriodicidadPago(id_empresa));
        
        return jsonretorno;
    }
    
    
    
    @RequestMapping(method = RequestMethod.POST, value="/getNomina.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getNominaJson(
            @RequestParam(value="identificador", required=true) Integer identificador,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        log.log(Level.INFO, "Ejecutando getPrefacturaJson de {0}", PrefacturasController.class.getName());
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        ArrayList<HashMap<String, Object>> datos = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> datosGrid = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> monedas = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> metodos_pago = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> periodicidad_pago = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> parametros = new ArrayList<HashMap<String, Object>>();
        
        ArrayList<HashMap<String,Object>>puestos=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>>departamentos=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> regimen_contratacion=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> tipo_contrato=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> tipo_jornada=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> riesgo_puesto=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> bancos=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> percepciones=new ArrayList<HashMap<String,Object>>();
        ArrayList<HashMap<String,Object>> deducciones=new ArrayList<HashMap<String,Object>>();
        
        ArrayList<HashMap<String, Object>> arrayExtra = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> extra = new HashMap<String, Object>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        
        
        
        
        ArrayList<HashMap<String, Object>> datosAdenda = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> valorIva = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> vendedores = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> condiciones = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> almacenes = new ArrayList<HashMap<String, Object>>();
        
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        
        if( identificador!=0  ){
            datos = this.getFacdao().getFacNomina_Datos(identificador);
            datosGrid = this.getFacdao().getFacNomina_Grid(identificador);
        }else{
            //Aquí solo entra cuando es nuevo
            //Obtiene los datos del Emisor y los almacena en el HashMap estra
            extra = this.getFacdao().getFacNomina_DatosEmisor(id_empresa);
            
            /*
            Es necesario conocer el id que tomará el registro antes guardar, 
            para eso se utiliza el metodo getIdSeqFacNomina para apartar el siguiente id de la secuencia,
            esto porque la nomina se de cada empleado se guardará uno por uno cada vez que se editen los datos y al guardar ya tenemos que conocer el id de la tabla header(fac_nomina).
            */
            //Agregar el nuevo ID para la tabla fac_nomina
            extra.put("identificador", this.getFacdao().getIdSeqFacNomina());
            
            arrayExtra.add(extra);
            
            //Obtiene parametros
            parametros = this.getFacdao().getFacNomina_Parametros(id_empresa, id_sucursal);
        }
        
        
        
        jsonretorno.put("Datos", datos);
        jsonretorno.put("datosGrid", datosGrid);
        jsonretorno.put("Monedas", this.getFacdao().getFactura_Monedas());
        jsonretorno.put("MetodosPago", this.getFacdao().getMetodosPago());
        jsonretorno.put("Periodicidad", this.getFacdao().getFacNomina_PeriodicidadPago(id_empresa));
        jsonretorno.put("Puestos", this.getFacdao().getFacNomina_Puestos(id_empresa));
        jsonretorno.put("Deptos", this.getFacdao().getFacNomina_Departamentos(id_empresa));
        jsonretorno.put("RegimenContrato", this.getFacdao().getFacNomina_RegimenContratacion());
        jsonretorno.put("TipoContrato",this.getFacdao().getFacNomina_TiposContrato());
        jsonretorno.put("TipoJornada",this.getFacdao().getFacNomina_TiposJornada());
        jsonretorno.put("Riesgos",this.getFacdao().getFacNomina_RiesgosPuesto());
        jsonretorno.put("Bancos",this.getFacdao().getFacNomina_Bancos(id_empresa));
        jsonretorno.put("ImpuestoRet",this.getFacdao().getFacNomina_ISR(id_empresa));
        jsonretorno.put("TiposHrsExtra",this.getFacdao().getFacNomina_TiposHoraExtra());
        jsonretorno.put("TiposIncapacidad",this.getFacdao().getFacNomina_TiposIncapacidad());
        
        //Solo debe obtener percepciones y deducciones de la Empresa sin tomar en cuenta el empleado
        Integer tipo=1;
        
        //Al enviar los primeros dos parametros en cero, solo obtiene las Dercepciones de la empresa sin filtrar por empleado
        jsonretorno.put("Percepciones",this.getFacdao().getFacNomina_Percepciones(tipo, 0,0, id_empresa));
        
        //Al enviar los primeros dos parametros en cero, solo obtiene las Deducciones de la empresa sin filtrar por empleado
        jsonretorno.put("Deducciones",this.getFacdao().getFacNomina_Deducciones(tipo,0,0, id_empresa));
        
        jsonretorno.put("Par", parametros);
        jsonretorno.put("Extra", arrayExtra);
        
        
        
        jsonretorno.put("datosAdenda", datosAdenda);
        jsonretorno.put("iva", valorIva);
        jsonretorno.put("Vendedores", vendedores);
        jsonretorno.put("Condiciones", condiciones);
        jsonretorno.put("Almacenes", almacenes);
        
        return jsonretorno;
    }
    
    
    
    
    //Obtiene todos los empleados que se les paga en el periodo indicado
    @RequestMapping(method = RequestMethod.POST, value="/getEmpleados.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getEmpleadosJson(
            @RequestParam(value="id", required=true) Integer periodicidad_id,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        //Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("Empleados", this.getFacdao().getFacNomina_Empleados(id_empresa, periodicidad_id));
        
        return jsonretorno;
    }
    
    
    //Obtiene todos los periodos de un Tipo de Periodicidad
    @RequestMapping(method = RequestMethod.POST, value="/getPeriodosPorTipoPeridicidad.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getPeriodosPorTipoPeridicidadJson(
            @RequestParam(value="id", required=true) Integer periodicidad_id,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        //Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("Periodos", this.getFacdao().getFacNomina_PeriodosPorTipo(periodicidad_id, id_empresa));
        
        return jsonretorno;
    }
    
    
    
    //Obtiene datos de la Nomina de un Empleado
    @RequestMapping(method = RequestMethod.POST, value="/getDataNominaEmpleado.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getDataNominaEmpleadoJson(
            @RequestParam(value="id_reg", required=true) Integer id_nom_det,
            @RequestParam(value="id_empleado", required=true) Integer id_empleado,
            @RequestParam(value="id_periodo", required=true) Integer id_periodo,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        //Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        Integer tipo=0;
        
        if(id_nom_det!=0){
            //Editar. Obtener datos de tabla de Nomina
            jsonretorno.put("Data", this.getFacdao().getFacNomina_DataNomina(id_nom_det, id_empleado));
            jsonretorno.put("HrsExtraEmpleado", this.getFacdao().getFacNomina_HorasExtras(id_nom_det));
            jsonretorno.put("IncapaEmpleado", this.getFacdao().getFacNomina_Incapacidades(id_nom_det));
            
            //Tipo=3, Obtener las Percepciones y Deducciones de la Nomina de un Periodo en especifico segun el id_reg
            tipo=3;
        }else{
            //Nuevo. Obtener datos de tabla de empleados
            jsonretorno.put("Data", this.getFacdao().getFacNomina_DataEmpleado(id_empleado));
            jsonretorno.put("Periodo", this.getFacdao().getFacNomina_DataPeriodo(id_periodo, id_empresa));
            
            //Tipo=2, Obtener las Percepciones y Deducciones configuradas en el catalogo de empleados
            tipo=2;
        }
        
        
        jsonretorno.put("PercepEmpleado", this.getFacdao().getFacNomina_Percepciones(tipo, id_nom_det, id_empleado, id_empresa));
        jsonretorno.put("DeducEmpleado", this.getFacdao().getFacNomina_Deducciones(tipo, id_nom_det, id_empleado, id_empresa));
        
        
        return jsonretorno;
    }
    
    
    
    
    //Edicion y nuevo
    @RequestMapping(method = RequestMethod.POST, value="/edit.json")
    public @ResponseBody HashMap<String, String> editJson(
            @RequestParam(value="accion", required=true) String accion,
            @RequestParam(value="nivel_ejecucion", required=true) String nivel_ejecucion,
            @RequestParam(value="identificador", required=true) Integer identificador,
            @RequestParam(value="comp_tipo", required=true) String comp_tipo,
            @RequestParam(value="comp_forma_pago", required=true) String comp_forma_pago,
            @RequestParam(value="comp_tc", required=true) String comp_tc,
            @RequestParam(value="comp_no_cuenta", required=true) String comp_no_cuenta,
            @RequestParam(value="fecha_pago", required=true) String fecha_pago,
            @RequestParam(value="select_comp_metodo_pago", required=true) String select_comp_metodo_pago,
            @RequestParam(value="select_comp_moneda", required=true) String select_comp_moneda,
            @RequestParam(value="select_comp_periodicidad", required=true) String select_comp_periodicidad,
            @RequestParam(value="select_no_periodo", required=true) String select_no_periodo,
            @RequestParam(value="elim", required=false) String[] elim,
            @RequestParam(value="id_reg", required=false) String[] id_reg,
            @RequestParam(value="id_emp", required=false) String[] id_empleado,
            @RequestParam(value="tpercep", required=false) String[] tpercep,
            @RequestParam(value="tdeduc", required=false) String[] tdeduc,
            @RequestParam(value="pago_neto", required=false) String[] pago_neto,
            
            @RequestParam(value="noTr", required=false) String[] noTr,
            @ModelAttribute("user") UserSessionData user
        ) throws Exception {
        
        //System.out.println(TimeHelper.getFechaActualYMDH()+": INICIO------------------------------------");
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        HashMap<String, String> parametros = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        HashMap<String,String> dataFacturaCliente = new HashMap<String,String>();
        ArrayList<LinkedHashMap<String,String>> conceptos = new ArrayList<LinkedHashMap<String,String>>();
        ArrayList<LinkedHashMap<String,String>> impTrasladados = new ArrayList<LinkedHashMap<String,String>>();
        ArrayList<LinkedHashMap<String,String>> impRetenidos = new ArrayList<LinkedHashMap<String,String>>();
        LinkedHashMap<String,String> datosExtrasXmlFactura = new LinkedHashMap<String,String>();
        LinkedHashMap<String,Object> dataAdenda = new LinkedHashMap<String,Object>();
        ArrayList<HashMap<String, String>> ieps = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> iva = new ArrayList<HashMap<String, String>>();
        LinkedHashMap<String,String> datosExtrasCfdi = new LinkedHashMap<String,String>();
        ArrayList<LinkedHashMap<String,String>> listaConceptosCfdi = new ArrayList<LinkedHashMap<String,String>>();
        ArrayList<LinkedHashMap<String,String>> impTrasladadosCfdi = new ArrayList<LinkedHashMap<String,String>>();
        ArrayList<LinkedHashMap<String,String>> impRetenidosCfdi = new ArrayList<LinkedHashMap<String,String>>();
        ArrayList<String> leyendas = new ArrayList<String>();
        
        HashMap<String,String> datos_emisor = new HashMap<String,String>();
        ArrayList<HashMap<String, String>> listaConceptosPdfCfd = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> datosExtrasPdfCfd= new HashMap<String, String>();
        
        String codeRespuesta="false";
        String msjRespuesta="";
        
        String retorno="";
        String tipo_facturacion="";
        String folio="";
        String serieFolio="";
        String rfcEmisor="";
        
        //Nomina
        Integer app_selected = 173;
        String command_selected = "";
        String succes_validation="";
        boolean actualizar_registro=true;
        String actualizo = "0";
        
        //Variable para el id  del usuario
        Integer id_usuario= user.getUserId();
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        String arreglo[];
        arreglo = new String[elim.length];
        
        for(int i=0; i<elim.length; i++) {
            arreglo[i]= "'"+elim[i] +"___" + noTr[i] +"___" + id_reg[i] +"___" + id_empleado[i] +"___"+ tpercep[i] +"___"+ tdeduc[i] +"___"+ pago_neto[i] +"'";
            //System.out.println(arreglo[i]);
        }
        
        //Serializar el arreglo
        String extra_data_array = StringUtils.join(arreglo, ",");
        
        command_selected = accion.trim().toLowerCase();
        
        String data_string = app_selected+"___"+command_selected+"___"+id_usuario+"___"+identificador+"___"+comp_tipo.toUpperCase()+"___"+comp_forma_pago.toUpperCase()+"___"+comp_tc+"___"+comp_no_cuenta+"___"+fecha_pago+"___"+select_comp_metodo_pago+"___"+select_comp_moneda+"___"+select_comp_periodicidad+"___"+select_no_periodo;
        //System.out.println("data_string: "+data_string);
        
        if(nivel_ejecucion.equals("2") && accion.equals("edit")){
            //Si el nivel de ejecucion es 2 y la accion es edit, no se tiene que actualizar el registro
            actualizar_registro=false;
        }
        
        if(actualizar_registro){
            if(command_selected.equals("new")){
                //Cuando es Nuevo, pasa sin validar
                succes_validation = "true";
            }else{
                //Cuando es diferente de Nuevo, se tiene que validar
                succes = this.getFacdao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
                succes_validation = succes.get("success");
                log.log(Level.INFO, TimeHelper.getFechaActualYMDH()+"Despues de validacion {0}", String.valueOf(succes.get("success")));
            }
            
            
            //System.out.println(TimeHelper.getFechaActualYMDH()+": Inicia actualizacion de datos de la prefactura");
            if( String.valueOf(succes_validation).equals("true")){

                retorno = this.getFacdao().selectFunctionForFacAdmProcesos(data_string, extra_data_array);

                //retorna un 1, si se  actualizo correctamente
                actualizo=retorno.split(":")[0];

                jsonretorno.put("actualizo",String.valueOf(actualizo));
                
                
                succes_validation="true";
                codeRespuesta="0";
                msjRespuesta="El registro se actualizo.";
            }
            
        }else{
            succes_validation="true";
            codeRespuesta="0";
            msjRespuesta="No se actualizo el registro.";
        }
        
        

        

        
        
        
        


        
        //System.out.println(TimeHelper.getFechaActualYMDH()+"::Termina Actualizacion de la Prefactura:: "+actualizo);
        /*
        if(actualizo.equals("1")){
            
            if ( !accion.equals("new") ){
                //select_tipo_documento 1=Factura, 3=Factura de Remision
                if(select_tipo_documento==1 || select_tipo_documento==3){
                    System.out.println(TimeHelper.getFechaActualYMDH()+"::::::::::::Iniciando Facturacion:::::::::::::::::..");
                    String proposito = "FACTURA";
                    
                    //obtener tipo de facturacion
                    tipo_facturacion = this.getFacdao().getTipoFacturacion(id_empresa);
                    tipo_facturacion = String.valueOf(tipo_facturacion);
                    
                    //Obtener el numero del PAC para el Timbrado de la Factura
                    String noPac = this.getFacdao().getNoPacFacturacion(id_empresa);
                    
                    //Obtener el Ambiente de Facturacion PRUEBAS ó PRODUCCION, solo aplica para Facturacion por Timbre FIscal(cfditf)
                    String ambienteFac = this.getFacdao().getAmbienteFacturacion(id_empresa);
                    
                    //System.out.println(TimeHelper.getFechaActualYMDH()+"::::::Tipo::"+tipo_facturacion+" | noPac::"+noPac+" | Ambiente::"+ambienteFac);
                    
                    //aqui se obtienen los parametros de la facturacion, nos intersa el tipo de formato para el pdf de la factura
                    parametros = this.getFacdao().getFac_Parametros(id_empresa, id_sucursal);

                    
                    //**********************************************************
                    //tipo facturacion CFDITF(CFDI TIMBRE FISCAL)
                    //**********************************************************
                    if(tipo_facturacion.equals("cfditf")){
                        
                        //Pac 0=Sin PAC, 1=Diverza, 2=ServiSim
                        if(!noPac.equals("0")){
                            //Solo se permite generar Factura para Timbrado con Diverza y ServiSim
                            //System.out.println(TimeHelper.getFechaActualYMDH()+":::::::::::Obteniendo datos para CFDI:::::::::::::::::..");
                            command_selected = "facturar_cfditf";
                            extra_data_array = "'sin datos'";
                            
                            //Obtener los valores del IEPS e IVAque se estan utilizando
                            ieps = this.getFacdao().getIeps(id_empresa);
                            iva = this.getFacdao().getIvas();
                            
                            
                            conceptos = this.getFacdao().getListaConceptosXmlCfdiTf(id_prefactura);
                            impRetenidos = this.getFacdao().getImpuestosRetenidosFacturaXml();
                            impTrasladados = this.getFacdao().getImpuestosTrasladadosFacturaXml(id_sucursal, conceptos, ieps, iva);
                            dataFacturaCliente = this.getFacdao().getDataFacturaXml(id_prefactura);
                            leyendas = this.getFacdao().getLeyendasEspecialesCfdi(id_empresa);
                            
                            //estos son requeridos para cfditf
                            datosExtrasXmlFactura.put("prefactura_id", String.valueOf(id_prefactura));
                            datosExtrasXmlFactura.put("tipo_documento", String.valueOf(select_tipo_documento));
                            datosExtrasXmlFactura.put("moneda_id", id_moneda);
                            datosExtrasXmlFactura.put("usuario_id", String.valueOf(id_usuario));
                            datosExtrasXmlFactura.put("empresa_id", String.valueOf(id_empresa));
                            datosExtrasXmlFactura.put("sucursal_id", String.valueOf(id_sucursal));
                            datosExtrasXmlFactura.put("refacturar", refacturar);
                            datosExtrasXmlFactura.put("app_selected", String.valueOf(app_selected));
                            datosExtrasXmlFactura.put("command_selected", command_selected);
                            datosExtrasXmlFactura.put("extra_data_array", extra_data_array);
                            datosExtrasXmlFactura.put("noPac", noPac);
                            datosExtrasXmlFactura.put("ambienteFac", ambienteFac);
                            
                            
                            //System.out.println(TimeHelper.getFechaActualYMDH()+":::::::::::Inicia BeanFacturador:::::::::::::::::..");
                            //genera xml factura
                            this.getBfCfdiTf().init(dataFacturaCliente, conceptos, impRetenidos, impTrasladados, proposito, datosExtrasXmlFactura, id_empresa, id_sucursal);
                            String timbrado_correcto = this.getBfCfdiTf().start();
                            //System.out.println(TimeHelper.getFechaActualYMDH()+":::::::::::Termina BeanFacturador:::::::::::::::::..");
                            String cadRes[] = timbrado_correcto.split("___");
                            
                            //aqui se checa si el xml fue validado correctamente
                            //si fue correcto debe traer un valor "true", de otra manera trae un error y ppor lo tanto no se genera el pdf
                            if(cadRes[0].equals("true")){
                                //obtiene serie_folio de la factura que se acaba de guardar
                                serieFolio = this.getFacdao().getSerieFolioFacturaByIdPrefactura(id_prefactura, id_empresa);
                                
                                String cadena_original=this.getBfCfdiTf().getCadenaOriginalTimbre();
                                //System.out.println("cadena_original:"+cadena_original);
                                
                                String sello_digital = this.getBfCfdiTf().getSelloDigital();
                                //System.out.println("sello_digital:"+sello_digital);
                                
                                //este es el timbre fiscal, se debe extraer del xml que nos devuelve el web service del timbrado
                                String sello_digital_sat = this.getBfCfdiTf().getSelloDigitalSat();
                                
                                //este es el folio fiscal del la factura timbrada, se obtiene   del xml
                                String uuid = this.getBfCfdiTf().getUuid();
                                String fechaTimbre = this.getBfCfdiTf().getFechaTimbrado();
                                String noCertSAT = this.getBfCfdiTf().getNoCertificadoSAT();
                                
                                //System.out.println(TimeHelper.getFechaActualYMDH()+":::::::::::Inicia construccion de PDF:::::::::::::::::..");
                                
                                //conceptos para el pdfcfd
                                listaConceptosPdfCfd = this.getFacdao().getListaConceptosPdfCfd(serieFolio);
                                
                                //datos para el pdf
                                datosExtrasPdfCfd = this.getFacdao().getDatosExtrasPdfCfd( serieFolio, proposito, cadena_original,sello_digital, id_sucursal);
                                datosExtrasPdfCfd.put("tipo_facturacion", tipo_facturacion);
                                datosExtrasPdfCfd.put("sello_sat", sello_digital_sat);
                                datosExtrasPdfCfd.put("uuid", uuid);
                                datosExtrasPdfCfd.put("fechaTimbre", fechaTimbre);
                                datosExtrasPdfCfd.put("noCertificadoSAT", noCertSAT);
                                
                                //pdf factura
                                if (parametros.get("formato_factura").equals("2")){
                                    pdfCfd_CfdiTimbradoFormato2 pdfFactura = new pdfCfd_CfdiTimbradoFormato2(this.getGralDao(), dataFacturaCliente, listaConceptosPdfCfd, leyendas, datosExtrasPdfCfd, id_empresa, id_sucursal);
                                    pdfFactura.ViewPDF();
                                }else{
                                    pdfCfd_CfdiTimbrado pdfFactura = new pdfCfd_CfdiTimbrado(this.getGralDao(), dataFacturaCliente, listaConceptosPdfCfd, datosExtrasPdfCfd, id_empresa, id_sucursal);
                                }
                                //System.out.println(TimeHelper.getFechaActualYMDH()+":::::::::::Termina construccion de PDF:::::::::::::::::..");
                                
                                
                                
                                jsonretorno.put("folio",serieFolio);
                                valorRespuesta="true";
                                //msjRespuesta=cadRes[1];
                                msjRespuesta = "Se gener&oacute; la Factura: "+serieFolio;
                                if (!procesoAdendaCorrecto){
                                    msjRespuesta = msjRespuesta + ", pero no fue posible agregar la Adenda.\nContacte a Soporte.";
                                }
                            }else{
                                valorRespuesta="false";
                                msjRespuesta=cadRes[1];
                            }
                        }else{
                            valorRespuesta="false";
                            msjRespuesta="No se puede Timbrar la Factura con el PAC actual.\nVerifique la configuraci&oacute;n del tipo de Facturaci&oacute;n y del PAC.";
                        }
                    }
                    
                }else{
                    valorRespuesta="true";
                    msjRespuesta="Se gener&oacute; la Remisi&oacute;n con Folio: "+jsonretorno.get("folio");
                }
                
            }else{
                if (accion.equals("new") ){
                    valorRespuesta="true";
                    msjRespuesta="El registro se gener&oacute; con &eacute;xito, puede proceder a Facturar.";
                }
            }//termina if accion diferente de new
            
            System.out.println("Folio: "+ String.valueOf(jsonretorno.get("folio")));
            
        }else{
            if(actualizo.equals("0")){
                jsonretorno.put("actualizo",String.valueOf(actualizo));
            }
        }
        */
        jsonretorno.put("success",succes_validation);
        jsonretorno.put("valor",codeRespuesta);
        jsonretorno.put("msj",msjRespuesta);
        
        System.out.println("Validacion: "+ String.valueOf(jsonretorno.get("success")));
        //System.out.println("Actualizo: "+String.valueOf(jsonretorno.get("actualizo")));
        System.out.println("codeRespuesta: "+String.valueOf(codeRespuesta));
        System.out.println("msjRespuesta: "+String.valueOf(msjRespuesta));
        
        //System.out.println(TimeHelper.getFechaActualYMDH()+": FIN------------------------------------");
        
        return jsonretorno;
    }
    
    
    
    
    
    //Edicion y nuevo de Nomina de Empleado
    @RequestMapping(method = RequestMethod.POST, value="/edit_nomina_empleado.json")
    public @ResponseBody HashMap<String, String> editNominaEmpleadoJson(
            @RequestParam(value="identificador", required=true) String identificador,
            @RequestParam(value="id_reg", required=true) String id_reg,
            @RequestParam(value="id_empleado", required=true) String id_empleado,
            @RequestParam(value="no_empleado", required=true) String no_empleado,
            @RequestParam(value="rfc_empleado", required=true) String rfc_empleado,
            @RequestParam(value="nombre_empleado", required=true) String nombre_empleado,
            @RequestParam(value="select_departamento", required=true) String select_departamento,
            @RequestParam(value="select_puesto", required=true) String select_puesto,
            @RequestParam(value="fecha_contrato", required=true) String fecha_contrato,
            @RequestParam(value="antiguedad", required=true) String antiguedad,
            @RequestParam(value="curp", required=true) String curp,
            @RequestParam(value="select_reg_contratacion", required=true) String select_reg_contratacion,
            @RequestParam(value="select_tipo_contrato", required=true) String select_tipo_contrato,
            @RequestParam(value="select_tipo_jornada", required=false) String select_tipo_jornada,
            @RequestParam(value="select_preriodo_pago", required=false) String select_preriodo_pago,
            @RequestParam(value="clabe", required=false) String clabe,
            @RequestParam(value="select_banco", required=false) String select_banco,
            @RequestParam(value="select_riesgo_puesto", required=false) String select_riesgo_puesto,
            @RequestParam(value="imss", required=false) String imss,
            @RequestParam(value="reg_patronal", required=false) String reg_patronal,
            @RequestParam(value="salario_base", required=false) String salario_base,
            @RequestParam(value="fecha_ini_pago", required=false) String fecha_ini_pago,
            @RequestParam(value="fecha_fin_pago", required=false) String fecha_fin_pago,
            @RequestParam(value="salario_integrado", required=false) String salario_integrado,
            @RequestParam(value="no_dias_pago", required=false) String no_dias_pago,
            @RequestParam(value="concepto_descripcion", required=false) String concepto_descripcion,
            @RequestParam(value="concepto_unidad", required=false) String concepto_unidad,
            @RequestParam(value="concepto_cantidad", required=false) String concepto_cantidad,
            @RequestParam(value="concepto_valor_unitario", required=false) String concepto_valor_unitario,
            @RequestParam(value="concepto_importe", required=false) String concepto_importe,
            @RequestParam(value="descuento", required=false) String descuento,
            @RequestParam(value="motivo_descuento", required=false) String motivo_descuento,
            @RequestParam(value="select_impuesto_retencion", required=false) String select_impuesto_retencion,
            @RequestParam(value="importe_retencion", required=false) String importe_retencion,
            @RequestParam(value="comp_subtotal", required=false) String comp_subtotal,
            @RequestParam(value="comp_descuento", required=false) String comp_descuento,
            @RequestParam(value="comp_retencion", required=false) String comp_retencion,
            @RequestParam(value="comp_total", required=false) String comp_total,
            @RequestParam(value="percep_total_gravado", required=false) String percep_total_gravado,
            @RequestParam(value="percep_total_excento", required=false) String percep_total_excento,
            @RequestParam(value="deduc_total_gravado", required=false) String deduc_total_gravado,
            @RequestParam(value="deduc_total_excento", required=false) String deduc_total_excento,
            @RequestParam(value="percepciones", required=false) String percepciones,
            @RequestParam(value="deducciones", required=false) String deducciones,
            @RequestParam(value="hrs_extras", required=false) String hrs_extras,
            @RequestParam(value="incapacidades", required=false) String incapacidades,
            @RequestParam(value="accion", required=false) String accion,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) throws Exception {
        
        System.out.println(TimeHelper.getFechaActualYMDH()+": INICIO-GUADAR NOMINA EMPLEADO------------------------------------");
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        HashMap<String, String> parametros = new HashMap<String, String>();
        

        
        String retorno="";
        String tipo_facturacion="";
        String folio="";
        String serieFolio="";
        String rfcEmisor="";
        
        //Nomina
        Integer app_selected = 173;
        String command_selected = "new_nomina";
        String extra_data_array = "'sin datos'";
        String succes_validation="";
        String codeRespuesta="";
        String msjRespuesta="";
        String actualizo = "0";
        
        
        //Decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        
        if(Integer.parseInt(id_reg)>0){
            command_selected = "edit_nomina";
        }else{
            if(accion.trim().toLowerCase().equals("edit")){
                command_selected = "edit_nomina";
            }
        }
        
        String data_string = 
                app_selected+"___"+
                command_selected+"___"+
                id_usuario+"___"+
                identificador+"___"+
                id_reg+"___"+
                id_empleado+"___"+
                no_empleado+"___"+
                rfc_empleado+"___"+
                nombre_empleado+"___"+
                select_departamento+"___"+
                select_puesto+"___"+
                fecha_contrato+"___"+
                antiguedad+"___"+
                curp+"___"+
                select_reg_contratacion+"___"+
                select_tipo_contrato+"___"+
                select_tipo_jornada+"___"+
                select_preriodo_pago+"___"+
                clabe+"___"+
                select_banco+"___"+
                select_riesgo_puesto+"___"+
                imss+"___"+
                reg_patronal+"___"+
                salario_base+"___"+
                fecha_ini_pago+"___"+
                fecha_fin_pago+"___"+
                salario_integrado+"___"+
                no_dias_pago+"___"+
                concepto_descripcion+"___"+
                concepto_unidad+"___"+
                concepto_cantidad+"___"+
                concepto_valor_unitario+"___"+
                concepto_importe+"___"+
                descuento+"___"+
                motivo_descuento+"___"+
                select_impuesto_retencion+"___"+
                importe_retencion+"___"+
                comp_subtotal+"___"+
                comp_descuento+"___"+
                comp_retencion+"___"+
                comp_total+"___"+
                percep_total_gravado+"___"+
                percep_total_excento+"___"+
                deduc_total_gravado+"___"+
                deduc_total_excento+"___"+
                percepciones+"___"+
                deducciones+"___"+
                hrs_extras+"___"+
                incapacidades;
        
        System.out.println("data_string_nomina_empleado: "+data_string);
        
        //Cuando es diferente de Nuevo, se tiene que validar
        succes = this.getFacdao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
        succes_validation = succes.get("success");
        
        if( String.valueOf(succes_validation).equals("true")){
            retorno = this.getFacdao().selectFunctionForFacAdmProcesos(data_string, extra_data_array);
            
            System.out.println("StringRetorno: "+retorno);
            
            //Retorna un 1, si se  actualizo correctamente
            actualizo = String.valueOf(retorno.split(":")[0]);
            
            //Retorna el id que se creó u actualizó
            jsonretorno.put("id",String.valueOf(retorno.split(":")[1]));
            
            codeRespuesta = String.valueOf(retorno.split(":")[2]);
            msjRespuesta = String.valueOf(retorno.split(":")[3]);
        }else{
            codeRespuesta="7001";
            msjRespuesta ="Error al intentar validar los datos.";
        }
        
        jsonretorno.put("success",succes_validation);
        jsonretorno.put("actualizo",actualizo);
        jsonretorno.put("valor",codeRespuesta);
        jsonretorno.put("msj",msjRespuesta);
        
        System.out.println("Validacion: "+ String.valueOf(jsonretorno.get("success")));
        System.out.println("codeRespuesta: "+String.valueOf(jsonretorno.get("valor")));
        System.out.println("msjRespuesta: "+String.valueOf(jsonretorno.get("msjRespuesta")));
        System.out.println(TimeHelper.getFechaActualYMDH()+": FIN-GUADAR NOMINA EMPLEADO------------------------------------");
        
        return jsonretorno;
    }
    
    
    
    
    
}
