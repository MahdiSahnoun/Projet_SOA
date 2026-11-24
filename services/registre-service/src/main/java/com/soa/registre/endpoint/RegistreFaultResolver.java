package com.soa.registre.endpoint;

import com.donsang.registre.v1.ErreurRegistre;
import com.soa.registre.exception.DonneurInconnuException;
import com.soa.registre.exception.DonneurInelegibleException;
import com.soa.registre.exception.GroupeSanguinInvalideException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.server.endpoint.SoapFaultAnnotationExceptionResolver;

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * T09 — Ajoute au SOAP Fault le détail déclaré dans le WSDL.
 *
 * faultcode et faultstring restent ceux de @SoapFault (Client + message de l'exception) :
 * le client Dons s'en sert pour choisir entre 404 et 409.
 *
 * En plus, le &lt;detail&gt; contient l'élément de fault de l'opération appelée
 * (XxxRequest → XxxFault, de type ErreurRegistre : code + message), comme le déclare
 * registre.xsd. Une opération sans fault déclaré (VerifierExistence) n'a pas de détail.
 *
 * Ordre HIGHEST_PRECEDENCE : passe avant le résolveur @SoapFault par défaut d'@EnableWs.
 */
@Component
public class RegistreFaultResolver extends SoapFaultAnnotationExceptionResolver {

    /** Éléments de fault déclarés dans registre.xsd (et donc dans le WSDL). */
    private static final Set<String> FAULTS_DECLARES = Set.of(
            "VerifierEligibiliteFault", "EnregistrerDonFault", "ListerDonneursEligiblesFault");

    private final JAXBContext jaxbContext;

    public RegistreFaultResolver() throws JAXBException {
        this.jaxbContext = JAXBContext.newInstance(ErreurRegistre.class);
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        String code = code(ex);
        String element = elementDeFault(endpoint);
        if (code == null || element == null) {
            return;
        }

        ErreurRegistre erreur = new ErreurRegistre();
        erreur.setCode(code);
        erreur.setMessage(ex.getMessage());

        JAXBElement<ErreurRegistre> detail = new JAXBElement<>(
                new QName(RegistreEndpoint.NAMESPACE, element), ErreurRegistre.class, erreur);
        try {
            jaxbContext.createMarshaller().marshal(detail, fault.addFaultDetail().getResult());
        } catch (JAXBException e) {
            throw new IllegalStateException("Impossible d'écrire le détail du SOAP Fault", e);
        }
    }

    /** VerifierEligibiliteRequest → VerifierEligibiliteFault, si ce fault est déclaré. */
    private static String elementDeFault(Object endpoint) {
        if (!(endpoint instanceof MethodEndpoint methodEndpoint)) {
            return null;
        }
        PayloadRoot payloadRoot = methodEndpoint.getMethod().getAnnotation(PayloadRoot.class);
        if (payloadRoot == null || !payloadRoot.localPart().endsWith("Request")) {
            return null;
        }
        String operation = payloadRoot.localPart().substring(0, payloadRoot.localPart().length() - "Request".length());
        String element = operation + "Fault";
        return FAULTS_DECLARES.contains(element) ? element : null;
    }

    private static String code(Exception ex) {
        if (ex instanceof DonneurInconnuException) {
            return "DONNEUR_INCONNU";
        }
        if (ex instanceof DonneurInelegibleException) {
            return "DONNEUR_INELIGIBLE";
        }
        if (ex instanceof GroupeSanguinInvalideException) {
            return "GROUPE_INVALIDE";
        }
        return null;
    }
}
