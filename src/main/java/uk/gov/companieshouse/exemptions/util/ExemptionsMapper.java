package uk.gov.companieshouse.exemptions.util;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.api.exemptions.LinksType;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.Updated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum.EXEMPTIONS;

@Component
public class ExemptionsMapper {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");

    public CompanyExemptionsDocument map(String companyNumber, InternalExemptionsApi requestBody) {
        return new CompanyExemptionsDocument()
                .setId(companyNumber)
                .setData(new CompanyExemptions()
                        .exemptions(requestBody.getExternalData().getExemptions())
                        .kind(EXEMPTIONS)
                        .links(new LinksType().self(String.format("/company/%s/exemptions", companyNumber)))
                        .etag(GenerateEtagUtil.generateEtag()))
                .setUpdated(new Updated(LocalDateTime.now()))
                .setDeltaAt(dateTimeFormatter.format(requestBody.getInternalData().getDeltaAt()));
    }
}
