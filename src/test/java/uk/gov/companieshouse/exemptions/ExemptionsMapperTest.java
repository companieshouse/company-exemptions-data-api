package uk.gov.companieshouse.exemptions;

import org.junit.Before;
import org.junit.Test;
import uk.gov.companieshouse.api.exemptions.*;
import uk.gov.companieshouse.api.model.exemptions.CompanyExemptionsApi;
import uk.gov.companieshouse.api.model.exemptions.ExemptionApi;
import uk.gov.companieshouse.api.model.exemptions.ExemptionItemsApi;
import uk.gov.companieshouse.api.model.exemptions.ExemptionsApi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum.EXEMPTIONS;
import static uk.gov.companieshouse.api.exemptions.DiclosureTransparencyRulesChapterFiveAppliesItem.ExemptionTypeEnum.DISCLOSURE_TRANSPARENCY_RULES_CHAPTER_FIVE_APPLIES;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsSharesAdmittedOnMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_SHARES_ADMITTED_ON_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnEuRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_EU_REGULATED_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnUkRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_UK_REGULATED_MARKET;

public class ExemptionsMapperTest {

    private static final String COMPANY_NUMBER = "123456789";

    private static final LocalDate DATE = LocalDate.of(2022, 11, 3);

    private ExemptionsMapper mapper;

    @Before
    public void setup() {
        mapper = new ExemptionsMapper();
    }

    @Test
    public void shouldTransformNaturalDisqualifiedOfficer() {
        InternalExemptionsApi requestBody = new InternalExemptionsApi();
        ExemptionsUpdateData external = new ExemptionsUpdateData();
        InternalData internal = new InternalData();
        Exemptions exemptions = new Exemptions();

        OffsetDateTime deltaAt = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1000, ZoneOffset.MIN);
        internal.setDeltaAt(deltaAt);
        internal.setUpdatedBy("example@ch.gov.uk");
        requestBody.setInternalData(internal);

        ExemptionItem exemptionItem = new ExemptionItem();
        exemptionItem.exemptFrom(DATE);
        exemptionItem.exemptTo(DATE);
        List<ExemptionItem> exemptionItems = Collections.singletonList(exemptionItem);

        PscExemptAsSharesAdmittedOnMarketItem schedule1RegisterMarket = new PscExemptAsSharesAdmittedOnMarketItem();
        DiclosureTransparencyRulesChapterFiveAppliesItem dtr5 = new DiclosureTransparencyRulesChapterFiveAppliesItem();
        PscExemptAsTradingOnEuRegulatedMarketItem euStateMarket = new PscExemptAsTradingOnEuRegulatedMarketItem();
        PscExemptAsTradingOnRegulatedMarketItem nonUkEeaStateMarket = new PscExemptAsTradingOnRegulatedMarketItem();
        PscExemptAsTradingOnUkRegulatedMarketItem ukEeaStateMarket = new PscExemptAsTradingOnUkRegulatedMarketItem();

        schedule1RegisterMarket.setItems(exemptionItems);
        schedule1RegisterMarket.setExemptionType(PSC_EXEMPT_AS_SHARES_ADMITTED_ON_MARKET);
        dtr5.setItems(exemptionItems);
        dtr5.setExemptionType(DISCLOSURE_TRANSPARENCY_RULES_CHAPTER_FIVE_APPLIES);
        euStateMarket.setItems(exemptionItems);
        euStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_EU_REGULATED_MARKET);
        nonUkEeaStateMarket.setItems(exemptionItems);
        nonUkEeaStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET);
        ukEeaStateMarket.setItems(exemptionItems);
        ukEeaStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_UK_REGULATED_MARKET);

        exemptions.setPscExemptAsSharesAdmittedOnMarket(schedule1RegisterMarket);
        exemptions.setDisclosureTransparencyRulesChapterFiveApplies(dtr5);
        exemptions.setPscExemptAsTradingOnEuRegulatedMarket(euStateMarket);
        exemptions.setPscExemptAsTradingOnRegulatedMarket(nonUkEeaStateMarket);
        exemptions.setPscExemptAsTradingOnUkRegulatedMarket(ukEeaStateMarket);

        external.setExemptions(exemptions);
        requestBody.setExternalData(external);

        CompanyExemptionsDocument document = mapper.map(COMPANY_NUMBER, requestBody);

        CompanyExemptions expectedData = new CompanyExemptions();

        expectedData.setExemptions(exemptions);
        expectedData.setKind(EXEMPTIONS);
        LinksType links = new LinksType();
        links.setSelf(String.format("/company/%s/exemptions", COMPANY_NUMBER));
        expectedData.setLinks(links);

        assertEquals(COMPANY_NUMBER, document.getId());
        assertNull(document.getCreated());
        assertNotNull(document.getData().getEtag());
        assertEquals(expectedData.getExemptions(), document.getData().getExemptions());
        assertEquals(expectedData.getKind(), document.getData().getKind());
        assertEquals(expectedData.getLinks(), document.getData().getLinks());
        assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }
}
