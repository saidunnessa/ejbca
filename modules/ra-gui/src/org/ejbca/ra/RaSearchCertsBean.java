/*************************************************************************
 *                                                                       *
 *  EJBCA Community: The OpenSource Certificate Authority                *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.cesecore.certificates.ca.CAInfo;
import org.cesecore.certificates.certificate.CertificateDataWrapper;
import org.cesecore.util.CertTools;
import org.cesecore.util.ValidityDate;
import org.ejbca.core.model.era.RaCertificateSearchRequest;
import org.ejbca.core.model.era.RaCertificateSearchResponse;
import org.ejbca.core.model.era.RaMasterApiProxyBeanLocal;

/**
 * Backing bean for Search Certificates page. 
 * 
 * @version $Id$
 */
@ManagedBean
@ViewScoped
public class RaSearchCertsBean implements Serializable {

    public class RaSearchCertificate {
        private final String fingerprint;
        private final String username;
        private final String subjectDn;
        private final String subjectAn;
        private final String caName;
        private final String created;
        private final String expires;
        public RaSearchCertificate(final CertificateDataWrapper cdw) {
            this.fingerprint = cdw.getCertificateData().getFingerprint();
            this.username = cdw.getCertificateData().getUsername();
            this.subjectDn = cdw.getCertificateData().getSubjectDN();
            this.subjectAn = CertTools.getSubjectAlternativeName(cdw.getCertificate());
            this.caName = caSubjectToNameMap.get(cdw.getCertificateData().getIssuerDN());
            this.created = ValidityDate.formatAsISO8601ServerTZ(CertTools.getNotBefore(cdw.getCertificate()).getTime(), TimeZone.getDefault());
            this.expires = ValidityDate.formatAsISO8601ServerTZ(cdw.getCertificateData().getExpireDate(), TimeZone.getDefault());
        }
        public String getFingerprint() { return fingerprint; }
        public String getUsername() { return username; }
        public String getSubjectDn() { return subjectDn; }
        public String getSubjectAn() { return subjectAn; }
        public String getCaName() { return caName; }
        public String getCreated() { return created; }
        public String getExpires() { return expires; }
    }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(RaSearchCertsBean.class);

    @EJB
    private RaMasterApiProxyBeanLocal raMasterApiProxyBean;

    @ManagedProperty(value="#{raAuthenticationBean}")
    private RaAuthenticationBean raAuthenticationBean;
    public void setRaAuthenticationBean(final RaAuthenticationBean raAuthenticationBean) { this.raAuthenticationBean = raAuthenticationBean; }

    @ManagedProperty(value="#{raLocaleBean}")
    private RaLocaleBean raLocaleBean;
    public void setRaLocaleBean(final RaLocaleBean raLocaleBean) { this.raLocaleBean = raLocaleBean; }

    private final List<RaSearchCertificate> resultsFiltered = new ArrayList<>();
    private List<CAInfo> caInfos = null;
    private Map<String,String> caSubjectToNameMap = new HashMap<>();

    private RaCertificateSearchRequest stagedRequest = new RaCertificateSearchRequest();
    private RaCertificateSearchRequest lastExecutedRequest = null;
    private RaCertificateSearchResponse lastExecutedResponse = null;

    public String getGenericSearchString() { return stagedRequest.getGenericSearchString(); }
    public void setGenericSearchString(final String genericSearchString) { stagedRequest.setGenericSearchString(genericSearchString); }
    
    public void searchAndFilterAction() {
        searchAndFilterCommon();
    }

    public void searchAndFilterAjaxListener(final AjaxBehaviorEvent event) {
        searchAndFilterCommon();
    }
    
    private void searchAndFilterCommon() {
        // TODO: Have a "current" request object that has functions for comparisons of all values with last request
        final int compared = stagedRequest.compareTo(lastExecutedRequest);
        boolean search = compared>0;
        if (compared<=0 && lastExecutedResponse!=null) {
            // More narrow search → filter and check if there are sufficient results left
            log.info("DEVELOP: More narrow or same → filter");
            filterTransformSort();
            // Check if there are sufficient results to fill screen and search for more
            if (resultsFiltered.size()<lastExecutedRequest.getMaxResults() && lastExecutedResponse.isMightHaveMoreResults()) {
                log.info("DEVELOP: Trying to load more results since filter left too few results");
                search = true;
            } else {
                search = false;
            }
        }
        if (search) {
            // Wider search → Query back-end
            log.info("DEVELOP: Wider → Query");
            lastExecutedResponse = raMasterApiProxyBean.searchForCertificates(raAuthenticationBean.getAuthenticationToken(), stagedRequest);
            lastExecutedRequest = stagedRequest;
            stagedRequest = new RaCertificateSearchRequest(stagedRequest);
            filterTransformSort();
        }
    }

    private void filterTransformSort() {
        resultsFiltered.clear();
        if (lastExecutedResponse != null) {
            for (final CertificateDataWrapper cdw : lastExecutedResponse.getCdws()) {
                if (!stagedRequest.getGenericSearchString().isEmpty() && (
                        (cdw.getCertificateData().getUsername() == null || !cdw.getCertificateData().getUsername().contains(stagedRequest.getGenericSearchString())) &&
                        (cdw.getCertificateData().getSubjectDN() == null || !cdw.getCertificateData().getSubjectDN().contains(stagedRequest.getGenericSearchString())))) {
                    continue;
                }
                if (!stagedRequest.getCaIds().isEmpty() && !stagedRequest.getCaIds().contains(cdw.getCertificateData().getIssuerDN().hashCode())) {
                    continue;
                }
                if (stagedRequest.getExpiresAfter()>0L) {
                    if (cdw.getCertificateData().getExpireDate()<stagedRequest.getExpiresAfter()) {
                        continue;
                    }
                }
                // if (this or that) { ...
                resultsFiltered.add(new RaSearchCertificate(cdw));
            }
            Collections.sort(resultsFiltered, new Comparator<RaSearchCertificate>() {
                @Override
                public int compare(RaSearchCertificate o1, RaSearchCertificate o2) {
                    return o1.username.compareTo(o2.username);
                }
            });
        }
    }
    
    public boolean isMoreResultsAvailable() {
        return lastExecutedResponse!=null && lastExecutedResponse.isMightHaveMoreResults();
    }

    public int getCriteriaCaId() {
        return stagedRequest.getCaIds().isEmpty() ? 0 : stagedRequest.getCaIds().get(0);
    }
    public void setCriteriaCaId(int criteriaCaId) {
        if (criteriaCaId==0) {
            stagedRequest.setCaIds(new ArrayList<Integer>());
        } else {
            stagedRequest.setCaIds(new ArrayList<>(Arrays.asList(new Integer[]{ criteriaCaId })));
        }
    }
    
    public boolean isCriteriaExcludeExpired() {
        return stagedRequest.getExpiresAfter()>0L;
    }
    public void setCriteriaExcludeExpired(final boolean value) {
        if (value) {
            stagedRequest.setExpiresAfter(System.currentTimeMillis());
        } else {
            stagedRequest.setExpiresAfter(0L);
        }
    }

    public List<SelectItem> getAvailableCas() {
        if (caInfos==null) {
            caInfos = new ArrayList<>(raMasterApiProxyBean.getAuthorizedCas(raAuthenticationBean.getAuthenticationToken()));
            Collections.sort(caInfos, new Comparator<CAInfo>() {
                @Override
                public int compare(final CAInfo caInfo1, final CAInfo caInfo2) {
                    return caInfo1.getName().compareTo(caInfo2.getName());
                }
            });
            for (final CAInfo caInfo : caInfos) {
                caSubjectToNameMap.put(caInfo.getSubjectDN(), caInfo.getName());
            }
        }
        final List<SelectItem> ret = new ArrayList<>();
        ret.add(new SelectItem(0, raLocaleBean.getMessage("search_certs_page_criteria_ca_optionany")));
        for (final CAInfo caInfo : caInfos) {
            ret.add(new SelectItem(caInfo.getCAId(), caInfo.getName()));
        }
        return ret;
    }

    public List<RaSearchCertificate> getFilteredResults() {
        return resultsFiltered;
    }
}