package uk.gov.companieshouse.exemptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.DiclosureTransparencyRulesChapterFiveAppliesItem;
import uk.gov.companieshouse.api.exemptions.ExemptionItem;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.api.exemptions.ExemptionsUpdateData;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.api.exemptions.LinksType;
import uk.gov.companieshouse.api.exemptions.PscExemptAsSharesAdmittedOnMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnEuRegulatedMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnUkRegulatedMarketItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum.EXEMPTIONS;
import static uk.gov.companieshouse.api.exemptions.DiclosureTransparencyRulesChapterFiveAppliesItem.ExemptionTypeEnum.DISCLOSURE_TRANSPARENCY_RULES_CHAPTER_FIVE_APPLIES;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsSharesAdmittedOnMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_SHARES_ADMITTED_ON_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnEuRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_EU_REGULATED_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET;
import static uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnUkRegulatedMarketItem.ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_UK_REGULATED_MARKET;

class ExemptionsMapperTest {

    private static final String COMPANY_NUMBER = "123456789";
    private static final LocalDate DATE = LocalDate.of(2022, 11, 3);

    private ExemptionsMapper mapper;

    @Before
    public void setup() {
        mapper = new ExemptionsMapper();
    }

    @Test
    @DisplayName("Test should successfully map an InternalExemptionsApi to a CompanyExemptionsDocument")
    public void map() {
        // Given
        ExemptionsUpdateData external = new ExemptionsUpdateData();
        external.setExemptions(getExemptions());

        InternalData internal = new InternalData();
        internal.setDeltaAt(OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1000, ZoneOffset.MIN));
        internal.setUpdatedBy("example@ch.gov.uk");

        InternalExemptionsApi requestBody = new InternalExemptionsApi();
        requestBody.setInternalData(internal);
        requestBody.setExternalData(external);

        CompanyExemptions expectedData = new CompanyExemptions();
        expectedData.setExemptions(getExemptions());
        expectedData.setKind(EXEMPTIONS);
        expectedData.setLinks(new LinksType().self(String.format("/company/%s/exemptions", COMPANY_NUMBER)));

        // When
        CompanyExemptionsDocument document = mapper.map(COMPANY_NUMBER, requestBody);

        // Then
        assertEquals(COMPANY_NUMBER, document.getId());
        assertNull(document.getCreated());
        assertNotNull(document.getData().getEtag());
        assertEquals(expectedData.getExemptions(), document.getData().getExemptions());
        assertEquals(expectedData.getKind(), document.getData().getKind());
        assertEquals(expectedData.getLinks(), document.getData().getLinks());
        assertTrue(LocalDateTime.now().toEpochSecond(ZoneOffset.MIN)
                - document.getUpdated().getAt().toEpochSecond(ZoneOffset.MIN) < 2);
    }

    private Exemptions getExemptions() {
        ExemptionItem exemptionItem = new ExemptionItem();
        exemptionItem.exemptFrom(DATE);
        exemptionItem.exemptTo(DATE);

        List<ExemptionItem> exemptionItems = Collections.singletonList(exemptionItem);

        PscExemptAsSharesAdmittedOnMarketItem schedule1RegisterMarket = new PscExemptAsSharesAdmittedOnMarketItem();
        schedule1RegisterMarket.setItems(exemptionItems);
        schedule1RegisterMarket.setExemptionType(PSC_EXEMPT_AS_SHARES_ADMITTED_ON_MARKET);

        DiclosureTransparencyRulesChapterFiveAppliesItem dtr5 = new DiclosureTransparencyRulesChapterFiveAppliesItem();
        dtr5.setItems(exemptionItems);
        dtr5.setExemptionType(DISCLOSURE_TRANSPARENCY_RULES_CHAPTER_FIVE_APPLIES);

        PscExemptAsTradingOnEuRegulatedMarketItem euStateMarket = new PscExemptAsTradingOnEuRegulatedMarketItem();
        euStateMarket.setItems(exemptionItems);
        euStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_EU_REGULATED_MARKET);

        PscExemptAsTradingOnRegulatedMarketItem nonUkEeaStateMarket = new PscExemptAsTradingOnRegulatedMarketItem();
        nonUkEeaStateMarket.setItems(exemptionItems);
        nonUkEeaStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET);

        PscExemptAsTradingOnUkRegulatedMarketItem ukEeaStateMarket = new PscExemptAsTradingOnUkRegulatedMarketItem();
        ukEeaStateMarket.setItems(exemptionItems);
        ukEeaStateMarket.setExemptionType(PSC_EXEMPT_AS_TRADING_ON_UK_REGULATED_MARKET);

        Exemptions exemptions = new Exemptions();
        exemptions.setPscExemptAsSharesAdmittedOnMarket(schedule1RegisterMarket);
        exemptions.setDisclosureTransparencyRulesChapterFiveApplies(dtr5);
        exemptions.setPscExemptAsTradingOnEuRegulatedMarket(euStateMarket);
        exemptions.setPscExemptAsTradingOnRegulatedMarket(nonUkEeaStateMarket);
        exemptions.setPscExemptAsTradingOnUkRegulatedMarket(ukEeaStateMarket);

        return exemptions;
    }
}
