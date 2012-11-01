/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */




package opendap.wcs.v2_0;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 10/30/12
 * Time: 11:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class NCName {






    public static void main(String[] args){



        testNcNameEscaping("200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("-200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("ioos/200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("_200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("A200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("__A200803061600_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("__A2008:03.06.16:00_HFRadar_USEGC_6km_rtv_SIO.ncml");
        testNcNameEscaping("ioos/2F200803061600_HFRadar_USEGC_6km_rtv_SIO.nc");




    }

    public static void testNcNameEscaping(String ncName){
        String encodedName = encodeNCNAME(ncName);
        String decodedName = decodeNCNAME(encodedName);

        System.out.append("\n");
        System.out.append("bad name:     ").append(ncName).append("\n");
        System.out.append("encoded name: ").append(encodedName).append("\n");
        System.out.append("decoded name: ").append(decodedName).append("\n");
        if(decodedName.equals(ncName))
            System.out.append("SUCCESS - decoded name matches original.\n");
        else
            System.out.append("ERROR - decoded name does not match original.\n");

        System.out.append("\n");

    }



    private static char ncnameEscapeChar = '_';
    private static String escapedEscapeChar = String.valueOf(ncnameEscapeChar)+String.valueOf(ncnameEscapeChar);




    /**
     * The practical restrictions of NCName are that it cannot contain several
     * symbol characters like :, @, $, %, &, /, +, ,, ;, whitespace characters
     * or different parenthesis. Furthermore an NCName cannot begin with a number,
     * dot or minus character although they can appear later in an NCName.
     * <p/>
     * <xsd:simpleType name="NCName" id="NCName">
     * <xsd:restriction base="xsd:Name">
     * <xsd:pattern value="[\i-[:]][\c-[:]]*"/>
     * </xsd:restriction>
     * </xsd:simpleType>
     *
     * @param s
     * @return
     */
    public static String encodeNCNAME(String s) {

        StringBuilder encodedName = new StringBuilder();

        int nameLength = s.length();

        if (nameLength == 0) {
            return s;
        }

        int index = 0;

        // Check first character
        char c = s.charAt(index);

        // prepend an escapeChar to the name
        if (c != '_' && !isLetter(c)) {
            encodedName.append(encodeNonNcNameChar(c));
            index++;
        }

        if (nameLength >1) {

            // Check the rest of the characters
            for (int i = index; i < nameLength; i++) {
                c = s.charAt(i);
                if (!isNCNameChar(c)) {
                    encodedName.append(encodeNonNcNameChar(c));
                }
                else if(c==ncnameEscapeChar){
                    encodedName.append(ncnameEscapeChar).append(ncnameEscapeChar);
                }
                else {
                    encodedName.append(c);
                }
            }
        }
        return encodedName.toString();
    }


    public static String decodeNCNAME(String s) {


        StringBuilder decodedName = new StringBuilder();

        int nameLength = s.length();
        int index = 0;

        if (nameLength == 0) {
            return s;
        }

        // Check first character
        char c = s.charAt(index);

        if(c=='_' && nameLength>1){

            c = s.charAt(index+1);
            if(c==ncnameEscapeChar){
                decodedName.append(ncnameEscapeChar);
                index += 2;
            }


        }

        boolean done = index>nameLength;
        while(!done){

            if(s.startsWith(escapedEscapeChar,index)){
                decodedName.append(ncnameEscapeChar);
                index += 2;
            }
            else if(s.startsWith(String.valueOf(ncnameEscapeChar),index)){

                String escapedChar = s.substring(index,index+5);

                decodedName.append(decodeEscapedNonNcNameChar(escapedChar));
                index += 5;
            }
            else {
                decodedName.append(s.charAt(index));
                index++;
            }
            done = index>=nameLength;
        }

        return decodedName.toString();
    }


    public static String encodeNonNcNameChar(char c) {

        return String.format("%c%04x", ncnameEscapeChar, (int) c);
    }

    public static char decodeEscapedNonNcNameChar(String  encodedChar) {

        String charString = encodedChar.substring(1,encodedChar.length());

        char decodedChar = (char) Integer.parseInt(charString, 16);

        return decodedChar;
    }

    public static boolean isEscapeChar(char c){
        return c==ncnameEscapeChar;
    }

    /**
     * Checks whether the supplied String is an NCName (Namespace Classified
     * Name) as specified at
     * <a href="http://www.w3.org/TR/REC-xml-names/#NT-NCName">
     * http://www.w3.org/TR/REC-xml-names/#NT-NCName</a>.
     */
    public static final boolean isNCName(String name) {
        int nameLength = name.length();

        if (nameLength == 0) {
            return false;
        }

        // Check first character
        char c = name.charAt(0);

        if (c == '_' || isLetter(c)) {
            // Check the rest of the characters
            for (int i = 1; i < nameLength; i++) {
                c = name.charAt(i);
                if (!isNCNameChar(c)) {
                    return false;
                }
            }

            // All characters have been checked
            return true;
        }

        return false;
    }

    public static final boolean isNCNameChar(char c) {
        return
                _isAsciiBaseChar(c) ||
                        _isAsciiDigit(c) ||
                        c == '.' || c == '-' || c == '_' ||
                        _isNonAsciiBaseChar(c) ||
                        _isNonAsciiDigit(c) ||
                        isIdeographic(c) ||
                        isCombiningChar(c) ||
                        isExtender(c);
    }

    public static final boolean isLetter(char c) {
        return
                _isAsciiBaseChar(c) ||
                        _isNonAsciiBaseChar(c) ||
                        isIdeographic(c);
    }

    private static final boolean _isAsciiBaseChar(char c) {
        return
                _charInRange(c, 0x0041, 0x005A) ||
                        _charInRange(c, 0x0061, 0x007A);
    }

    private static final boolean _isNonAsciiBaseChar(char c) {
        return
                _charInRange(c, 0x00C0, 0x00D6) ||
                        _charInRange(c, 0x00D8, 0x00F6) ||
                        _charInRange(c, 0x00F8, 0x00FF) ||
                        _charInRange(c, 0x0100, 0x0131) ||
                        _charInRange(c, 0x0134, 0x013E) ||
                        _charInRange(c, 0x0141, 0x0148) ||
                        _charInRange(c, 0x014A, 0x017E) ||
                        _charInRange(c, 0x0180, 0x01C3) ||
                        _charInRange(c, 0x01CD, 0x01F0) ||
                        _charInRange(c, 0x01F4, 0x01F5) ||
                        _charInRange(c, 0x01FA, 0x0217) ||
                        _charInRange(c, 0x0250, 0x02A8) ||
                        _charInRange(c, 0x02BB, 0x02C1) ||
                        c == 0x0386 ||
                        _charInRange(c, 0x0388, 0x038A) ||
                        c == 0x038C ||
                        _charInRange(c, 0x038E, 0x03A1) ||
                        _charInRange(c, 0x03A3, 0x03CE) ||
                        _charInRange(c, 0x03D0, 0x03D6) ||
                        c == 0x03DA ||
                        c == 0x03DC ||
                        c == 0x03DE ||
                        c == 0x03E0 ||
                        _charInRange(c, 0x03E2, 0x03F3) ||
                        _charInRange(c, 0x0401, 0x040C) ||
                        _charInRange(c, 0x040E, 0x044F) ||
                        _charInRange(c, 0x0451, 0x045C) ||
                        _charInRange(c, 0x045E, 0x0481) ||
                        _charInRange(c, 0x0490, 0x04C4) ||
                        _charInRange(c, 0x04C7, 0x04C8) ||
                        _charInRange(c, 0x04CB, 0x04CC) ||
                        _charInRange(c, 0x04D0, 0x04EB) ||
                        _charInRange(c, 0x04EE, 0x04F5) ||
                        _charInRange(c, 0x04F8, 0x04F9) ||
                        _charInRange(c, 0x0531, 0x0556) ||
                        c == 0x0559 ||
                        _charInRange(c, 0x0561, 0x0586) ||
                        _charInRange(c, 0x05D0, 0x05EA) ||
                        _charInRange(c, 0x05F0, 0x05F2) ||
                        _charInRange(c, 0x0621, 0x063A) ||
                        _charInRange(c, 0x0641, 0x064A) ||
                        _charInRange(c, 0x0671, 0x06B7) ||
                        _charInRange(c, 0x06BA, 0x06BE) ||
                        _charInRange(c, 0x06C0, 0x06CE) ||
                        _charInRange(c, 0x06D0, 0x06D3) ||
                        c == 0x06D5 ||
                        _charInRange(c, 0x06E5, 0x06E6) ||
                        _charInRange(c, 0x0905, 0x0939) ||
                        c == 0x093D ||
                        _charInRange(c, 0x0958, 0x0961) ||
                        _charInRange(c, 0x0985, 0x098C) ||
                        _charInRange(c, 0x098F, 0x0990) ||
                        _charInRange(c, 0x0993, 0x09A8) ||
                        _charInRange(c, 0x09AA, 0x09B0) ||
                        c == 0x09B2 ||
                        _charInRange(c, 0x09B6, 0x09B9) ||
                        _charInRange(c, 0x09DC, 0x09DD) ||
                        _charInRange(c, 0x09DF, 0x09E1) ||
                        _charInRange(c, 0x09F0, 0x09F1) ||
                        _charInRange(c, 0x0A05, 0x0A0A) ||
                        _charInRange(c, 0x0A0F, 0x0A10) ||
                        _charInRange(c, 0x0A13, 0x0A28) ||
                        _charInRange(c, 0x0A2A, 0x0A30) ||
                        _charInRange(c, 0x0A32, 0x0A33) ||
                        _charInRange(c, 0x0A35, 0x0A36) ||
                        _charInRange(c, 0x0A38, 0x0A39) ||
                        _charInRange(c, 0x0A59, 0x0A5C) ||
                        c == 0x0A5E ||
                        _charInRange(c, 0x0A72, 0x0A74) ||
                        _charInRange(c, 0x0A85, 0x0A8B) ||
                        c == 0x0A8D ||
                        _charInRange(c, 0x0A8F, 0x0A91) ||
                        _charInRange(c, 0x0A93, 0x0AA8) ||
                        _charInRange(c, 0x0AAA, 0x0AB0) ||
                        _charInRange(c, 0x0AB2, 0x0AB3) ||
                        _charInRange(c, 0x0AB5, 0x0AB9) ||
                        c == 0x0ABD ||
                        c == 0x0AE0 ||
                        _charInRange(c, 0x0B05, 0x0B0C) ||
                        _charInRange(c, 0x0B0F, 0x0B10) ||
                        _charInRange(c, 0x0B13, 0x0B28) ||
                        _charInRange(c, 0x0B2A, 0x0B30) ||
                        _charInRange(c, 0x0B32, 0x0B33) ||
                        _charInRange(c, 0x0B36, 0x0B39) ||
                        c == 0x0B3D ||
                        _charInRange(c, 0x0B5C, 0x0B5D) ||
                        _charInRange(c, 0x0B5F, 0x0B61) ||
                        _charInRange(c, 0x0B85, 0x0B8A) ||
                        _charInRange(c, 0x0B8E, 0x0B90) ||
                        _charInRange(c, 0x0B92, 0x0B95) ||
                        _charInRange(c, 0x0B99, 0x0B9A) ||
                        c == 0x0B9C ||
                        _charInRange(c, 0x0B9E, 0x0B9F) ||
                        _charInRange(c, 0x0BA3, 0x0BA4) ||
                        _charInRange(c, 0x0BA8, 0x0BAA) ||
                        _charInRange(c, 0x0BAE, 0x0BB5) ||
                        _charInRange(c, 0x0BB7, 0x0BB9) ||
                        _charInRange(c, 0x0C05, 0x0C0C) ||
                        _charInRange(c, 0x0C0E, 0x0C10) ||
                        _charInRange(c, 0x0C12, 0x0C28) ||
                        _charInRange(c, 0x0C2A, 0x0C33) ||
                        _charInRange(c, 0x0C35, 0x0C39) ||
                        _charInRange(c, 0x0C60, 0x0C61) ||
                        _charInRange(c, 0x0C85, 0x0C8C) ||
                        _charInRange(c, 0x0C8E, 0x0C90) ||
                        _charInRange(c, 0x0C92, 0x0CA8) ||
                        _charInRange(c, 0x0CAA, 0x0CB3) ||
                        _charInRange(c, 0x0CB5, 0x0CB9) ||
                        c == 0x0CDE ||
                        _charInRange(c, 0x0CE0, 0x0CE1) ||
                        _charInRange(c, 0x0D05, 0x0D0C) ||
                        _charInRange(c, 0x0D0E, 0x0D10) ||
                        _charInRange(c, 0x0D12, 0x0D28) ||
                        _charInRange(c, 0x0D2A, 0x0D39) ||
                        _charInRange(c, 0x0D60, 0x0D61) ||
                        _charInRange(c, 0x0E01, 0x0E2E) ||
                        c == 0x0E30 ||
                        _charInRange(c, 0x0E32, 0x0E33) ||
                        _charInRange(c, 0x0E40, 0x0E45) ||
                        _charInRange(c, 0x0E81, 0x0E82) ||
                        c == 0x0E84 ||
                        _charInRange(c, 0x0E87, 0x0E88) ||
                        c == 0x0E8A ||
                        c == 0x0E8D ||
                        _charInRange(c, 0x0E94, 0x0E97) ||
                        _charInRange(c, 0x0E99, 0x0E9F) ||
                        _charInRange(c, 0x0EA1, 0x0EA3) ||
                        c == 0x0EA5 ||
                        c == 0x0EA7 ||
                        _charInRange(c, 0x0EAA, 0x0EAB) ||
                        _charInRange(c, 0x0EAD, 0x0EAE) ||
                        c == 0x0EB0 ||
                        _charInRange(c, 0x0EB2, 0x0EB3) ||
                        c == 0x0EBD ||
                        _charInRange(c, 0x0EC0, 0x0EC4) ||
                        _charInRange(c, 0x0F40, 0x0F47) ||
                        _charInRange(c, 0x0F49, 0x0F69) ||
                        _charInRange(c, 0x10A0, 0x10C5) ||
                        _charInRange(c, 0x10D0, 0x10F6) ||
                        c == 0x1100 ||
                        _charInRange(c, 0x1102, 0x1103) ||
                        _charInRange(c, 0x1105, 0x1107) ||
                        c == 0x1109 ||
                        _charInRange(c, 0x110B, 0x110C) ||
                        _charInRange(c, 0x110E, 0x1112) ||
                        c == 0x113C ||
                        c == 0x113E ||
                        c == 0x1140 ||
                        c == 0x114C ||
                        c == 0x114E ||
                        c == 0x1150 ||
                        _charInRange(c, 0x1154, 0x1155) ||
                        c == 0x1159 ||
                        _charInRange(c, 0x115F, 0x1161) ||
                        c == 0x1163 ||
                        c == 0x1165 ||
                        c == 0x1167 ||
                        c == 0x1169 ||
                        _charInRange(c, 0x116D, 0x116E) ||
                        _charInRange(c, 0x1172, 0x1173) ||
                        c == 0x1175 ||
                        c == 0x119E ||
                        c == 0x11A8 ||
                        c == 0x11AB ||
                        _charInRange(c, 0x11AE, 0x11AF) ||
                        _charInRange(c, 0x11B7, 0x11B8) ||
                        c == 0x11BA ||
                        _charInRange(c, 0x11BC, 0x11C2) ||
                        c == 0x11EB ||
                        c == 0x11F0 ||
                        c == 0x11F9 ||
                        _charInRange(c, 0x1E00, 0x1E9B) ||
                        _charInRange(c, 0x1EA0, 0x1EF9) ||
                        _charInRange(c, 0x1F00, 0x1F15) ||
                        _charInRange(c, 0x1F18, 0x1F1D) ||
                        _charInRange(c, 0x1F20, 0x1F45) ||
                        _charInRange(c, 0x1F48, 0x1F4D) ||
                        _charInRange(c, 0x1F50, 0x1F57) ||
                        c == 0x1F59 ||
                        c == 0x1F5B ||
                        c == 0x1F5D ||
                        _charInRange(c, 0x1F5F, 0x1F7D) ||
                        _charInRange(c, 0x1F80, 0x1FB4) ||
                        _charInRange(c, 0x1FB6, 0x1FBC) ||
                        c == 0x1FBE ||
                        _charInRange(c, 0x1FC2, 0x1FC4) ||
                        _charInRange(c, 0x1FC6, 0x1FCC) ||
                        _charInRange(c, 0x1FD0, 0x1FD3) ||
                        _charInRange(c, 0x1FD6, 0x1FDB) ||
                        _charInRange(c, 0x1FE0, 0x1FEC) ||
                        _charInRange(c, 0x1FF2, 0x1FF4) ||
                        _charInRange(c, 0x1FF6, 0x1FFC) ||
                        c == 0x2126 ||
                        _charInRange(c, 0x212A, 0x212B) ||
                        c == 0x212E ||
                        _charInRange(c, 0x2180, 0x2182) ||
                        _charInRange(c, 0x3041, 0x3094) ||
                        _charInRange(c, 0x30A1, 0x30FA) ||
                        _charInRange(c, 0x3105, 0x312C) ||
                        _charInRange(c, 0xAC00, 0xD7A3);
    }

    public static final boolean isIdeographic(char c) {
        return
                _charInRange(c, 0x4E00, 0x9FA5) ||
                        c == 0x3007 ||
                        _charInRange(c, 0x3021, 0x3029);
    }

    public static final boolean isCombiningChar(char c) {
        return
                _charInRange(c, 0x0300, 0x0345) ||
                        _charInRange(c, 0x0360, 0x0361) ||
                        _charInRange(c, 0x0483, 0x0486) ||
                        _charInRange(c, 0x0591, 0x05A1) ||
                        _charInRange(c, 0x05A3, 0x05B9) ||
                        _charInRange(c, 0x05BB, 0x05BD) ||
                        c == 0x05BF ||
                        _charInRange(c, 0x05C1, 0x05C2) ||
                        c == 0x05C4 ||
                        _charInRange(c, 0x064B, 0x0652) ||
                        c == 0x0670 ||
                        _charInRange(c, 0x06D6, 0x06DC) ||
                        _charInRange(c, 0x06DD, 0x06DF) ||
                        _charInRange(c, 0x06E0, 0x06E4) ||
                        _charInRange(c, 0x06E7, 0x06E8) ||
                        _charInRange(c, 0x06EA, 0x06ED) ||
                        _charInRange(c, 0x0901, 0x0903) ||
                        c == 0x093C ||
                        _charInRange(c, 0x093E, 0x094C) ||
                        c == 0x094D ||
                        _charInRange(c, 0x0951, 0x0954) ||
                        _charInRange(c, 0x0962, 0x0963) ||
                        _charInRange(c, 0x0981, 0x0983) ||
                        c == 0x09BC ||
                        c == 0x09BE ||
                        c == 0x09BF ||
                        _charInRange(c, 0x09C0, 0x09C4) ||
                        _charInRange(c, 0x09C7, 0x09C8) ||
                        _charInRange(c, 0x09CB, 0x09CD) ||
                        c == 0x09D7 ||
                        _charInRange(c, 0x09E2, 0x09E3) ||
                        c == 0x0A02 ||
                        c == 0x0A3C ||
                        c == 0x0A3E ||
                        c == 0x0A3F ||
                        _charInRange(c, 0x0A40, 0x0A42) ||
                        _charInRange(c, 0x0A47, 0x0A48) ||
                        _charInRange(c, 0x0A4B, 0x0A4D) ||
                        _charInRange(c, 0x0A70, 0x0A71) ||
                        _charInRange(c, 0x0A81, 0x0A83) ||
                        c == 0x0ABC ||
                        _charInRange(c, 0x0ABE, 0x0AC5) ||
                        _charInRange(c, 0x0AC7, 0x0AC9) ||
                        _charInRange(c, 0x0ACB, 0x0ACD) ||
                        _charInRange(c, 0x0B01, 0x0B03) ||
                        c == 0x0B3C ||
                        _charInRange(c, 0x0B3E, 0x0B43) ||
                        _charInRange(c, 0x0B47, 0x0B48) ||
                        _charInRange(c, 0x0B4B, 0x0B4D) ||
                        _charInRange(c, 0x0B56, 0x0B57) ||
                        _charInRange(c, 0x0B82, 0x0B83) ||
                        _charInRange(c, 0x0BBE, 0x0BC2) ||
                        _charInRange(c, 0x0BC6, 0x0BC8) ||
                        _charInRange(c, 0x0BCA, 0x0BCD) ||
                        c == 0x0BD7 ||
                        _charInRange(c, 0x0C01, 0x0C03) ||
                        _charInRange(c, 0x0C3E, 0x0C44) ||
                        _charInRange(c, 0x0C46, 0x0C48) ||
                        _charInRange(c, 0x0C4A, 0x0C4D) ||
                        _charInRange(c, 0x0C55, 0x0C56) ||
                        _charInRange(c, 0x0C82, 0x0C83) ||
                        _charInRange(c, 0x0CBE, 0x0CC4) ||
                        _charInRange(c, 0x0CC6, 0x0CC8) ||
                        _charInRange(c, 0x0CCA, 0x0CCD) ||
                        _charInRange(c, 0x0CD5, 0x0CD6) ||
                        _charInRange(c, 0x0D02, 0x0D03) ||
                        _charInRange(c, 0x0D3E, 0x0D43) ||
                        _charInRange(c, 0x0D46, 0x0D48) ||
                        _charInRange(c, 0x0D4A, 0x0D4D) ||
                        c == 0x0D57 ||
                        c == 0x0E31 ||
                        _charInRange(c, 0x0E34, 0x0E3A) ||
                        _charInRange(c, 0x0E47, 0x0E4E) ||
                        c == 0x0EB1 ||
                        _charInRange(c, 0x0EB4, 0x0EB9) ||
                        _charInRange(c, 0x0EBB, 0x0EBC) ||
                        _charInRange(c, 0x0EC8, 0x0ECD) ||
                        _charInRange(c, 0x0F18, 0x0F19) ||
                        c == 0x0F35 ||
                        c == 0x0F37 ||
                        c == 0x0F39 ||
                        c == 0x0F3E ||
                        c == 0x0F3F ||
                        _charInRange(c, 0x0F71, 0x0F84) ||
                        _charInRange(c, 0x0F86, 0x0F8B) ||
                        _charInRange(c, 0x0F90, 0x0F95) ||
                        c == 0x0F97 ||
                        _charInRange(c, 0x0F99, 0x0FAD) ||
                        _charInRange(c, 0x0FB1, 0x0FB7) ||
                        c == 0x0FB9 ||
                        _charInRange(c, 0x20D0, 0x20DC) ||
                        c == 0x20E1 ||
                        _charInRange(c, 0x302A, 0x302F) ||
                        c == 0x3099 ||
                        c == 0x309A;
    }

    public static final boolean isDigit(char c) {
        return
                _isAsciiDigit(c) ||
                        _isNonAsciiDigit(c);
    }

    private static final boolean _isAsciiDigit(char c) {
        return
                _charInRange(c, 0x0030, 0x0039);
    }

    private static final boolean _isNonAsciiDigit(char c) {
        return
                _charInRange(c, 0x0660, 0x0669) ||
                        _charInRange(c, 0x06F0, 0x06F9) ||
                        _charInRange(c, 0x0966, 0x096F) ||
                        _charInRange(c, 0x09E6, 0x09EF) ||
                        _charInRange(c, 0x0A66, 0x0A6F) ||
                        _charInRange(c, 0x0AE6, 0x0AEF) ||
                        _charInRange(c, 0x0B66, 0x0B6F) ||
                        _charInRange(c, 0x0BE7, 0x0BEF) ||
                        _charInRange(c, 0x0C66, 0x0C6F) ||
                        _charInRange(c, 0x0CE6, 0x0CEF) ||
                        _charInRange(c, 0x0D66, 0x0D6F) ||
                        _charInRange(c, 0x0E50, 0x0E59) ||
                        _charInRange(c, 0x0ED0, 0x0ED9) ||
                        _charInRange(c, 0x0F20, 0x0F29);
    }

    public static final boolean isExtender(char c) {
        return
                c == 0x00B7 ||
                        c == 0x02D0 ||
                        c == 0x02D1 ||
                        c == 0x0387 ||
                        c == 0x0640 ||
                        c == 0x0E46 ||
                        c == 0x0EC6 ||
                        c == 0x3005 ||
                        _charInRange(c, 0x3031, 0x3035) ||
                        _charInRange(c, 0x309D, 0x309E) ||
                        _charInRange(c, 0x30FC, 0x30FE);
    }

    private static final boolean _charInRange(char c, int start, int end) {
        return c >= start && c <= end;
    }


}
