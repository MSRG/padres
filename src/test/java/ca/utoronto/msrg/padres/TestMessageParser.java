package ca.utoronto.msrg.padres;

import org.junit.Before;
import org.junit.After;

import org.junit.Test;

import org.junit.Assert;
// FIXME include in TestSuite @RunWith(Suite.class)@Suite.SuiteClasses(...)
import ca.utoronto.msrg.padres.common.message.*;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.message.parser.TokenMgrError;

public class TestMessageParser extends Assert {

    @Test
    public void test_pub_should_contain_minus_value() throws ParseException, TokenMgrError {

        String stringRep = new String("[class,'temp'],[area,'tor'],[value,-10];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[value,-10]"));
    }

    @Test
    public void test_pub_should_contain_positive_value() throws ParseException {
        String stringRep = new String("[class,'temp'],[area,'tor'],[value,+10];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[value,10]"));
    }

    @Test
    public void test_pub_should_contain_negative_value_with_komma() throws ParseException {
        String stringRep = new String("[class,'temp'],[area,'tor'],[value,-10.9];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[value,-10.9]"));
    }

    @Test
    public void test_pub_should_contain_positive_value_with_komma() throws ParseException {
        String stringRep = new String("[class,'temp'],[area,'tor'],[value,+10.9];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[value,10.9]"));
    }

    @Test
    public void test_pub_should_contain_price_tuple() throws ParseException {
        String stringRep = new String("[class,stock],[price,100.3];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[price,100.3]"));
    }

    @Test
    public void test_pub_should_contain_text_value() throws ParseException {
        String stringRep = new String("[class,stock],[price,\"100.4\"];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[price,\"100.4\"]"));
    }

    @Test
    public void test_pub_should_parse_class_correct() throws ParseException {
        String stringRep = new String("[class,reading];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,reading]"));
    }

    @Test
    public void test_pub_should_parse_class_tuple_correct() throws ParseException {
        String stringRep = new String("[class,abc];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,abc]"));
    }

    @Test
    public void test_pub_should_parse_class_and_attribute_correct() throws ParseException {
        String stringRep = new String("[class,reading],[level,123];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,reading]"));
        assertTrue(pub.toString().contains("[level,123]"));
    }

    @Test
    public void test_pub_should_parse_and_escape_text_value() throws ParseException {
        String stringRep = new String("[class,reading],[level,'abc'];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,reading]"));
        assertTrue(pub.toString().contains("[level,\"abc\"]"));
    }

    @Test
    public void test_pub_should_parse_and_escape_triple() throws ParseException {
        String stringRep = new String("[class,reading],[shipID,123],[level,5];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,reading]"));
        assertTrue(pub.toString().contains("[shipID,123]"));
        assertTrue(pub.toString().contains("[level,5]"));
    }

    @Test
    public void test_pub_should_parse_text_as_text_in_triple() throws ParseException {
        String stringRep = new String("[class,audit],[firm,ACME],[trust,5];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,audit]"));
        assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
        assertTrue(pub.toString().contains("[trust,5]"));
    }

    @Test
    public void test_should_parse_four_tuple_with_text_correct() throws ParseException {
        String stringRep = new String("[class,manifest],[shipID,123],[firm,ACME],[content,dynamite];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,manifest],"));
        assertTrue(pub.toString().contains("[shipID,123]"));
        assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
        assertTrue(pub.toString().contains("[content,\"dynamite\"]"));
    }

    @Test
    public void test_should_parse_mixed_types_1() throws ParseException {
        String stringRep = new String("[class,audit],[firm,3.3],[trust,5];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,audit]"));
        assertTrue(pub.toString().contains("[firm,3.3]"));
        assertTrue(pub.toString().contains("[trust,5]"));
    }

    @Test
    public void test_should_parse_mixed_types_2() throws ParseException {
        String stringRep = new String("[class,manifest],[shipID,123],[firm,\"ACME\"],[content,4.5];");
        Publication pub = MessageFactory.createPublicationFromString(stringRep);
        assertTrue(pub.toString().contains("[class,manifest]"));
        assertTrue(pub.toString().contains("[shipID,123]"));
        assertTrue(pub.toString().contains("[firm,\"ACME\"]"));
        assertTrue(pub.toString().contains("[content,4.5]"));
    }

    @Test
    public void test_should_parse_equal_sign_correct_with_numbers() throws ParseException {
        String stringRep = "[class,eq,'stock'],[price,=,100.3];";
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[price,=,100.3]"));
    }

    @Test
    public void test_should_parse_broker_control_correct() throws ParseException {
        String stringRep = "[class,eq,'BROKER_CONTROL'],[brokerID,isPresent,''],[command,str-contains,'-'],[broker,isPresent,''],[fromID,isPresent,''],[fromURI,isPresent,''];";
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[brokerID,isPresent,\"\"]"));
    }

    @Test
    public void test_should_parse_equal_sign_correct_with_classes() throws ParseException {
        String stringRep = new String("[class,eq,reading];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
    }

    @Test
    public void test_should_parse_tuple_with_komma_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,=,5];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,=,5]"));
    }

    @Test
    public void should_parse_escaped_number_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,eq,\"5\"];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,eq,\"5\"]"));
    }

    @Test
    public void should_parse_komma_in_level_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,=,5.5];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,=,5.5]"));
    }


    @Test
    public void should_parse_equal_() throws ParseException {

        String stringRep = new String("[class,eq,reading];");
        System.out.println("Input: " + stringRep);
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
    }

    @Test
    public void schould_parse_isPresent_with_number() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,isPresent,123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,isPresent,123]"));
    }

    @Test
    public void should_parse_is_present_with_string() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,isPresent,abc];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,isPresent,\"abc\"]"));
    }

    @Test
    public void should_parse_is_present_with_double_value() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,isPresent,123.123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[level,isPresent,123.123]"));
    }

    @Test
    public void should_parse_with_comma() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,=,123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,=,123]"));
    }

    @Test
    public void should_parse_is_present_with_number() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,isPresent,123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,isPresent,123]"));
    }

    @Test
    public void should_parse_is_present_with_string_unescaped() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,isPresent,abc];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,isPresent,\"abc\"]"));
    }

    @Test
    public void should_parse_bigger_and_equals() throws ParseException {
        String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,audit]"));
        assertTrue(adv.toString().contains("[firm,isPresent,\"abc\"]"));
        assertTrue(adv.toString().contains("[trust,>=,0]"));
    }

    @Test
    public void should_parse_multiple_isPresent_correct() throws ParseException {
        String stringRep = new String("[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,manifest]"));
        assertTrue(adv.toString().contains("[shipID,isPresent,123]"));
        assertTrue(adv.toString().contains("[firm,isPresent,123.123]"));
        assertTrue(adv.toString().contains("[content,isPresent,\"abc\"]"));

    }

    @Test
    public void should_parse_bigger_sign() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,>,123]"));
    }

    @Test
    public void should_parse_equals_with_numer_and_smaller_sign() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,=,123],[level,<,3];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,=,123]"));
        assertTrue(adv.toString().contains("[level,<,3]"));
    }

    @Test
    public void should_parse_bigger_equals_and_smaller_equals() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,<=,123],[level,>=,3];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,<=,123]"));
        assertTrue(adv.toString().contains("[level,>=,3]"));
    }

    @Test
    public void should_parse_isPresent_and_bigger() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123],[level,isPresent,\"123\"];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,>,123]"));
        assertTrue(adv.toString().contains("[level,isPresent,\"123\"]"));

    }

    @Test
    public void should_parse_bigger_float_number() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123.456];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,>,123.456]"));
    }

    @Test
    public void should_parse_smaller_float_and_bigger_float() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,<=,123.567]"));
        assertTrue(adv.toString().contains("[level,>=,3.456]"));
    }

    @Test
    public void should_parse_bigger_float() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,=,123.456],[level,<,3.456];");
        System.out.println("Input: " + stringRep);
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,=,123.456]"));
        assertTrue(adv.toString().contains("[level,<,3.456]"));
    }

    @Test
    public void should_parse_bigger_and_ispresent() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123.345],[level,isPresent,123.123];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,>,123.345]"));
        assertTrue(adv.toString().contains("[level,isPresent,123.123]"));
    }

    @Test
    public void should_parse_str_gt() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,str-gt,\"abc\"]"));
    }

    @Test
    public void should_parse_str_lt() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,eq,\"abc\"]"));
        assertTrue(adv.toString().contains("[level,str-lt,\"abcd\"]"));
    }

    @Test
    public void should_parse_str_ge_and_str_le() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,str-le,\"abc\"]"));
        assertTrue(adv.toString().contains("[level,str-ge,\"abcd\"]"));
    }

    @Test
    public void should_parse_str_gt_and_isPresent() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,reading]"));
        assertTrue(adv.toString().contains("[shipID,str-gt,\"abc\"]"));
        assertTrue(adv.toString().contains("[level,isPresent,\"abc\"]"));
    }

    @Test
    public void testCreateTableMixedTypes() throws ParseException {
        String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,audit]"));
        assertTrue(adv.toString().contains("[firm,isPresent,\"abc\"]"));
        assertTrue(adv.toString().contains("[trust,>=,0.0]"));
    }

    @Test
    public void should_parse_eq_and_other_operators() throws ParseException {
        String stringRep = new String("[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67];");
        Advertisement adv = MessageFactory.createAdvertisementFromString(stringRep);
        assertTrue(adv.toString().contains("[class,eq,manifest]"));
        assertTrue(adv.toString().contains("[shipID,=,12345]"));
        assertTrue(adv.toString().contains("[content,eq,\"radioactivestuff\"]"));
        assertTrue(adv.toString().contains("[firm,>,12345.67]"));
    }

    @Test
    public void should_parse_price_with_prependend_komma() throws ParseException {
        String stringRep = "[class,eq,'stock'],[price,=,100.3];";
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[price,=,100.3]"));
    }

    @Test
    public void should_parse_equal() throws ParseException {
        String stringRep = new String("[class,eq,reading];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
    }

    @Test
    public void should_parse_equal_and_equal_with_number() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,=,5];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[level,=,5]"));
    }

    @Test
    public void should_parse_equal_and_escaped_number() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,eq,\"5\"];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[level,eq,\"5\"]"));
    }

    @Test
    public void should_parse_equal_and_number_with_prepended_komma() throws ParseException {
        String stringRep = new String("[class,eq,reading],[level,=,5.5];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[level,=,5.5]"));
    }

    @Test
    public void should_parse_select_on_multiple_properties() throws ParseException {
        String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,audit]"));
        assertTrue(sub.toString().contains("[firm,isPresent,\"abc\"]"));
        assertTrue(sub.toString().contains("[trust,>=,0]"));
    }

    @Test
    public void should_parse_select_multiple_properties_and_isPresent() throws ParseException {
        String stringRep = new String("[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,manifest]"));
        assertTrue(sub.toString().contains("[shipID,isPresent,123]"));
        assertTrue(sub.toString().contains("[firm,isPresent,123.123]"));
        assertTrue(sub.toString().contains("[content,isPresent,\"abc\"]"));
    }

    @Test
    public void testSelectIntWithDifferentOperators() throws ParseException {

        String stringRep = new String("[class,eq,reading],[shipID,>,123];");
        System.out.println("Input: " + stringRep);
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,>,123]"));
    }

    @Test
    public void should_select_equal_integer() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,=,123],[level,<,3];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,=,123]"));
        assertTrue(sub.toString().contains("[level,<,3]"));
    }

    @Test
    public void should_select_smaller_integer() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,<=,123],[level,>=,3];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,<=,123]"));
        assertTrue(sub.toString().contains("[level,>=,3]"));
    }

    @Test
    public void should_select_bigger_integer() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123],[level,isPresent,123];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,>,123]"));
        assertTrue(sub.toString().contains("[level,isPresent,123]"));
    }

    @Test
    public void testSelectDoubleWithDifferentOperators() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123.456];");
        System.out.println("Input: " + stringRep);
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,>,123.456]"));
    }

    @Test
    public void should_parse_equals_with_double() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,=,123.456],[level,<,3.456];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,=,123.456]"));
        assertTrue(sub.toString().contains("[level,<,3.456]"));
    }

    @Test
    public void should_parse_bigger_with_double() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,<=,123.567]"));
        assertTrue(sub.toString().contains("[level,>=,3.456]"));
    }

    @Test
    public void should_parse_smaller_with_double() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,>,123.345],[level,isPresent,\"123.123\"];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,>,123.345]"));
        assertTrue(sub.toString().contains("[level,isPresent,\"123.123\"]"));
    }

    @Test
    public void should_parse_string_with_str_gt() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc];");
        System.out.println("Input: " + stringRep);
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,str-gt,\"abc\"]"));
    }

    @Test
    public void should_pares_string_with_str_lt_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,eq,\"abc\"]"));
        assertTrue(sub.toString().contains("[level,str-lt,\"abcd\"]"));
    }

    @Test
    public void should_parse_string_with_str_ge_comparison_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,str-le,\"abc\"]"));
        assertTrue(sub.toString().contains("[level,str-ge,\"abcd\"]"));
    }

    @Test
    public void should_parse_string_with_str_gt_and_isPresent_correct() throws ParseException {
        String stringRep = new String("[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,reading]"));
        assertTrue(sub.toString().contains("[shipID,str-gt,\"abc\"]"));
        assertTrue(sub.toString().contains("[level,isPresent,\"abc\"]"));
    }
    
    @Test
    public void testSelectMixedTypes_1() throws ParseException {
        String stringRep = new String("[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0];");
        System.out.println("Input: " + stringRep);
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,audit]"));
        assertTrue(sub.toString().contains("[firm,isPresent,\"abc\"]"));
        assertTrue(sub.toString().contains("[trust,>=,0.0]"));
    }

    @Test
    public void testSelectMixedTypes_2() throws ParseException {
        String stringRep = new String("[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67];");
        Subscription sub = MessageFactory.createSubscriptionFromString(stringRep);
        assertTrue(sub.toString().contains("[class,eq,manifest]"));
        assertTrue(sub.toString().contains("[shipID,=,12345]"));
        assertTrue(sub.toString().contains("[content,eq,\"radioactivestuff\"]"));
        assertTrue(sub.toString().contains("[firm,>,12345.67]"));
    }

    @Test
    public void testCompositeSelect() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,=,5]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,=,5]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,eq,\"5\"]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,eq,\"5\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,=,5.5]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,=,5.5]"));
        }
    }

    @Test
    public void testCompositeSelectAsteriskTypes() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,isPresent,123]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,abc]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,isPresent,\"abc\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[level,isPresent,123.123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,isPresent,123.123]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,=,123]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,isPresent,123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,isPresent,123]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,isPresent,abc]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,isPresent,\"abc\"]"));
        }
    }

    @Test
    public void testCompositeSelectMultipleAttributes() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,audit],[firm,isPresent,abc],[trust,>=,0]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[firm,isPresent,\"abc\"]"));
            assertTrue(comsub.toString().contains("[trust,>=,0]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,manifest],[shipID,isPresent,123],[firm,isPresent,123.123],[content,isPresent,abc]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,manifest]"));
            assertTrue(comsub.toString().contains("[shipID,isPresent,123]"));
            assertTrue(comsub.toString().contains("[firm,isPresent,123.123]"));
            assertTrue(comsub.toString().contains("[content,isPresent,\"abc\"]"));
        }
    }

    @Test
    public void testCompositeSelectIntWithDifferentOperators() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,>,123]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123],[level,<,3]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,=,123]"));
            assertTrue(comsub.toString().contains("[level,<,3]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,<=,123],[level,>=,3]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,<=,123]"));
            assertTrue(comsub.toString().contains("[level,>=,3]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123],[level,isPresent,123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,>,123]"));
            assertTrue(comsub.toString().contains("[level,isPresent,123]"));
        }
    }

    @Test
    public void testCompositeSelectDoubleWithDifferentOperators() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123.456]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,>,123.456]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,=,123.456],[level,<,3.456]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,=,123.456]"));
            assertTrue(comsub.toString().contains(",[level,<,3.456]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,<=,123.567],[level,>=,3.456]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,<=,123.567]"));
            assertTrue(comsub.toString().contains("[level,>=,3.456]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,>,123.345],[level,isPresent,123.123]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,>,123.345]"));
            assertTrue(comsub.toString().contains("[level,isPresent,123.123]"));
        }
    }

    @Test
    public void testCompositeSelectStringWithDifferentOperators() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-gt,abc]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,str-gt,\"abc\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,eq,abc],[level,str-lt,abcd]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,eq,\"abc\"]"));
            assertTrue(comsub.toString().contains("[level,str-lt,\"abcd\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-le,abc],[level,str-ge,abcd]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,str-le,\"abc\"]"));
            assertTrue(comsub.toString().contains("[level,str-ge,\"abcd\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,reading],[shipID,str-gt,abc],[level,isPresent,abc]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[shipID,str-gt,\"abc\"]"));
            assertTrue(comsub.toString().contains("[level,isPresent,\"abc\"]"));
        }
    }

    @Test
    public void testCompositeSelectMixedTypes() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,audit],[firm,isPresent,abc],[trust,>=,0.0]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[firm,isPresent,\"abc\"]"));
            assertTrue(comsub.toString().contains("[trust,>=,0.0]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit]}&{[class,eq,manifest],[shipID,=,12345],[content,eq,radioactivestuff],[firm,>,12345.67]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,manifest]"));
            assertTrue(comsub.toString().contains("[shipID,=,12345]"));
            assertTrue(comsub.toString().contains("[content,eq,\"radioactivestuff\"]"));
            assertTrue(comsub.toString().contains("[firm,>,12345.67]"));
        }
    }

    @Test
    public void testCompositeSelectVariablesEqualling() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,audit],[reading,=,$I$X0]}&{[class,eq,reading],[reading,=,$I$X0]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[reading,=,\"$I$X0\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit],[reading,=,$F$X0]}&{[class,eq,reading],[reading,=,$F$X0]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[reading,=,\"$F$X0\"]"));
        }

        {
            String stringRep = new String("{{[class,eq,audit],[reading,eq,$S$X0]}&{[class,eq,reading],[reading,eq,$S$X0]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[reading,eq,\"$S$X0\"]"));
        }

        {
            String stringRep = new String("{{{[class,eq,audit]}&{[class,eq,audit]}}&{[class,eq,audit]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().substring(10).contains("[class,eq,audit]"));
            assertTrue(comsub.toString().substring(25).contains("[class,eq,audit]"));
        }

        {
            String stringRep = new String("{{{[class,eq,audit],[reading,=,$I$X2]}&{[class,eq,audit],[reading,=,$I$X2],[value,=,$F$X3]}}&{[class,eq,audit],[value,=,$F$X3]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[reading,=,\"$I$X2\"]"));
            assertTrue(comsub.toString().contains("[value,=,\"$F$X3\"]"));
        }
    }


    @Test
    public void testCompositeSelectBracesChecking() throws ParseException {
        {
            String stringRep = new String("{{{{[class,eq,fa]}&{{[class,eq,do]}||{[class,eq,re]}}}&{[class,eq,so]}}||{[class,eq,do]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,fa]"));
            assertTrue(comsub.toString().contains("[class,eq,do]"));
            assertTrue(comsub.toString().contains("[class,eq,re]"));
            assertTrue(comsub.toString().contains("[class,eq,so]"));
            assertTrue(comsub.toString().contains("[class,eq,do]"));
        }

        {
            String stringRep = new String("{{{{[class,eq,do]}&{{{[class,eq,re]}||{[class,eq,mi]}}&{[class,eq,fa]}}}||{[class,eq,so]}}&{{{[class,eq,la]}||{[class,eq,ti]}}&{[class,eq,do]}}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,do]"));
            assertTrue(comsub.toString().contains("[class,eq,re]"));
            assertTrue(comsub.toString().contains("[class,eq,mi]"));
            assertTrue(comsub.toString().contains("[class,eq,fa]"));
            assertTrue(comsub.toString().contains("[class,eq,so]"));
            assertTrue(comsub.toString().contains("[class,eq,la]"));
            assertTrue(comsub.toString().contains("[class,eq,ti]"));
            assertTrue(comsub.toString().contains("[class,eq,do]"));
        }
    }

    @Test
    public void test1CompositeSelect() throws ParseException {
        {
            String stringRep = new String("{{[class,eq,reading],[level,>,3]}&{[class,eq,audit],[trust,>,7]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,>,3]"));
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[trust,>,7]"));
        }

        {
            String stringRep = new String("{{[class,eq,reading],[level,>,3]}&{[class,eq,audit],[trust,>,7]}}");
            System.out.println("Input: " + stringRep);
            CompositeSubscription comsub = MessageFactory.createCompositeSubscriptionFromString(stringRep);
            assertTrue(comsub.toString().contains("[class,eq,reading]"));
            assertTrue(comsub.toString().contains("[level,>,3]"));
            assertTrue(comsub.toString().contains("[class,eq,audit]"));
            assertTrue(comsub.toString().contains("[trust,>,7]"));
        }
    }
}