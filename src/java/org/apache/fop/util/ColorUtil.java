/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.util;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlgraphics.java2d.color.ColorExt;
import org.apache.xmlgraphics.java2d.color.ColorSpaces;
import org.apache.xmlgraphics.java2d.color.DeviceCMYKColorSpace;
import org.apache.xmlgraphics.java2d.color.ICCColor;
import org.apache.xmlgraphics.java2d.color.NamedColorSpace;
import org.apache.xmlgraphics.java2d.color.profile.NamedColorProfile;
import org.apache.xmlgraphics.java2d.color.profile.NamedColorProfileParser;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Generic Color helper class.
 * <p>
 * This class supports parsing string values into color values and creating
 * color values for strings. It provides a list of standard color names.
 */
public final class ColorUtil {

    //Implementation note: this class should ALWAYS create ColorExt instances instead of using
    //java.awt.Color since the latter has an equals() method that can't detect two different
    //colors using the same sRGB fallback.

    /** The name for the uncalibrated CMYK pseudo-profile */
    public static final String CMYK_PSEUDO_PROFILE = "#CMYK";

    /** The name for the Separation pseudo-profile used for spot colors */
    public static final String SEPARATION_PSEUDO_PROFILE = "#Separation";

    /**
     *
     * keeps all the predefined and parsed colors.
     * <p>
     * This map is used to predefine given colors, as well as speeding up
     * parsing of already parsed colors.
     */
    private static Map colorMap = null;

    /** Logger instance */
    protected static Log log = LogFactory.getLog(ColorUtil.class);

    static {
        initializeColorMap();
    }

    /**
     * Private constructor since this is an utility class.
     */
    private ColorUtil() {
    }

    /**
     * Creates a color from a given string.
     * <p>
     * This function supports a wide variety of inputs.
     * <ul>
     * <li>#RGB (hex 0..f)</li>
     * <li>#RGBA (hex 0..f)</li>
     * <li>#RRGGBB (hex 00..ff)</li>
     * <li>#RRGGBBAA (hex 00..ff)</li>
     * <li>rgb(r,g,b) (0..255 or 0%..100%)</li>
     * <li>java.awt.Color[r=r,g=g,b=b] (0..255)</li>
     * <li>system-color(colorname)</li>
     * <li>transparent</li>
     * <li>colorname</li>
     * <li>fop-rgb-icc(r,g,b,cs,cs-src,[num]+) (r/g/b: 0..1, num: 0..1)</li>
     * <li>cmyk(c,m,y,k) (0..1)</li>
     * </ul>
     *
     * @param foUserAgent FOUserAgent object
     * @param value
     *            the string to parse.
     * @return a Color representing the string if possible
     * @throws PropertyException
     *             if the string is not parsable or does not follow any of the
     *             given formats.
     */
    public static Color parseColorString(FOUserAgent foUserAgent, String value)
            throws PropertyException {
        if (value == null) {
            return null;
        }

        Color parsedColor = (Color) colorMap.get(value.toLowerCase());

        if (parsedColor == null) {
            if (value.startsWith("#")) {
                parsedColor = parseWithHash(value);
            } else if (value.startsWith("rgb(")) {
                parsedColor = parseAsRGB(value);
            } else if (value.startsWith("url(")) {
                throw new PropertyException(
                        "Colors starting with url( are not yet supported!");
            } else if (value.startsWith("java.awt.Color")) {
                parsedColor = parseAsJavaAWTColor(value);
            } else if (value.startsWith("system-color(")) {
                parsedColor = parseAsSystemColor(value);
            } else if (value.startsWith("fop-rgb-icc")) {
                parsedColor = parseAsFopRgbIcc(foUserAgent, value);
            } else if (value.startsWith("fop-rgb-named-color")) {
                parsedColor = parseAsFopRgbNamedColor(foUserAgent, value);
            } else if (value.startsWith("cmyk")) {
                parsedColor = parseAsCMYK(value);
            }

            if (parsedColor == null) {
                throw new PropertyException("Unknown Color: " + value);
            }

            colorMap.put(value, parsedColor);
        }

        // TODO - Returned Color object can be one from the static colorMap cache.
        //        That means it should be treated as read only for the rest of its lifetime.
        //        Not sure that is the case though.
        return parsedColor;
    }

    /**
     * Tries to parse a color given with the system-color() function.
     *
     * @param value
     *            the complete line
     * @return a color if possible
     * @throws PropertyException
     *             if the format is wrong.
     */
    private static Color parseAsSystemColor(String value)
            throws PropertyException {
        int poss = value.indexOf("(");
        int pose = value.indexOf(")");
        if (poss != -1 && pose != -1) {
            value = value.substring(poss + 1, pose);
        } else {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be system-color(x)");
        }
        return (Color) colorMap.get(value);
    }

    /**
     * Tries to parse the standard java.awt.Color toString output.
     *
     * @param value
     *            the complete line
     * @return a color if possible
     * @throws PropertyException
     *             if the format is wrong.
     * @see java.awt.Color#toString()
     */
    private static Color parseAsJavaAWTColor(String value)
            throws PropertyException {
        float red = 0.0f, green = 0.0f, blue = 0.0f;
        int poss = value.indexOf("[");
        int pose = value.indexOf("]");
        try {
            if (poss != -1 && pose != -1) {
                value = value.substring(poss + 1, pose);
                String[] args = value.split(",");
                if (args.length != 3) {
                    throw new PropertyException(
                            "Invalid number of arguments for a java.awt.Color: " + value);
                }

                red = Float.parseFloat(args[0].trim().substring(2)) / 255f;
                green = Float.parseFloat(args[1].trim().substring(2)) / 255f;
                blue = Float.parseFloat(args[2].trim().substring(2)) / 255f;
                if ((red < 0.0 || red > 1.0)
                        || (green < 0.0 || green > 1.0)
                        || (blue < 0.0 || blue > 1.0)) {
                    throw new PropertyException("Color values out of range");
                }
            } else {
                throw new IllegalArgumentException(
                            "Invalid format for a java.awt.Color: " + value);
            }
        } catch (PropertyException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PropertyException(e);
        }
        return new ColorExt(red, green, blue, null);
    }

    /**
     * Parse a color given with the rgb() function.
     *
     * @param value
     *            the complete line
     * @return a color if possible
     * @throws PropertyException
     *             if the format is wrong.
     */
    private static Color parseAsRGB(String value) throws PropertyException {
        Color parsedColor;
        int poss = value.indexOf("(");
        int pose = value.indexOf(")");
        if (poss != -1 && pose != -1) {
            value = value.substring(poss + 1, pose);
            try {
                String[] args = value.split(",");
                if (args.length != 3) {
                    throw new PropertyException(
                            "Invalid number of arguments: rgb(" + value + ")");
                }
                float red = 0.0f, green = 0.0f, blue = 0.0f;
                String str = args[0].trim();
                if (str.endsWith("%")) {
                    red = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100f;
                } else {
                    red = Float.parseFloat(str) / 255f;
                }
                str = args[1].trim();
                if (str.endsWith("%")) {
                    green = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100f;
                } else {
                    green = Float.parseFloat(str) / 255f;
                }
                str = args[2].trim();
                if (str.endsWith("%")) {
                    blue = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100f;
                } else {
                    blue = Float.parseFloat(str) / 255f;
                }
                if ((red < 0.0 || red > 1.0)
                        || (green < 0.0 || green > 1.0)
                        || (blue < 0.0 || blue > 1.0)) {
                    throw new PropertyException("Color values out of range");
                }
                parsedColor = new ColorExt(red, green, blue, null);
            } catch (PropertyException pe) {
                //simply re-throw
                throw pe;
            } catch (Exception e) {
                //wrap in a PropertyException
                throw new PropertyException(e);
            }
        } else {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be rgb(r,g,b)");
        }
        return parsedColor;
    }

    /**
     * parse a color given in the #.... format.
     *
     * @param value
     *            the complete line
     * @return a color if possible
     * @throws PropertyException
     *             if the format is wrong.
     */
    private static Color parseWithHash(String value) throws PropertyException {
        Color parsedColor = null;
        try {
            int len = value.length();
            int alpha;
            if (len == 5 || len == 9) {
                alpha = Integer.parseInt(
                        value.substring((len == 5) ? 3 : 7), 16);
            } else {
                alpha = 0xFF;
            }
            int red = 0, green = 0, blue = 0;
            if ((len == 4) || (len == 5)) {
                //multiply by 0x11 = 17 = 255/15
                red = Integer.parseInt(value.substring(1, 2), 16) * 0x11;
                green = Integer.parseInt(value.substring(2, 3), 16) * 0x11;
                blue = Integer.parseInt(value.substring(3, 4), 16) * 0X11;
            } else if ((len == 7) || (len == 9)) {
                red = Integer.parseInt(value.substring(1, 3), 16);
                green = Integer.parseInt(value.substring(3, 5), 16);
                blue = Integer.parseInt(value.substring(5, 7), 16);
            } else {
                throw new NumberFormatException();
            }
            parsedColor = new ColorExt(red, green, blue, alpha, null);
        } catch (Exception e) {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be #RGB. #RGBA, #RRGGBB, or #RRGGBBAA");
        }
        return parsedColor;
    }

    /**
     * Parse a color specified using the fop-rgb-icc() function.
     *
     * @param value the function call
     * @return a color if possible
     * @throws PropertyException if the format is wrong.
     */
    private static Color parseAsFopRgbIcc(FOUserAgent foUserAgent, String value)
            throws PropertyException {
        Color parsedColor;
        int poss = value.indexOf("(");
        int pose = value.indexOf(")");
        if (poss != -1 && pose != -1) {
            String[] args = value.substring(poss + 1, pose).split(",");

            try {
                if (args.length < 5) {
                    throw new PropertyException("Too few arguments for rgb-icc() function");
                }

                //Set up fallback sRGB value
                float red = 0, green = 0, blue = 0;
                red = Float.parseFloat(args[0].trim());
                green = Float.parseFloat(args[1].trim());
                blue = Float.parseFloat(args[2].trim());
                /* Verify rgb replacement arguments */
                if ((red < 0 || red > 1)
                        || (green < 0 || green > 1)
                        || (blue < 0 || blue > 1)) {
                    throw new PropertyException("Color values out of range. "
                            + "Fallback RGB arguments to fop-rgb-icc() must be [0..1]");
                }
                Color sRGB = new ColorExt(red, green, blue, null);

                /* Get and verify ICC profile name */
                String iccProfileName = args[3].trim();
                if (iccProfileName == null || "".equals(iccProfileName)) {
                    throw new PropertyException("ICC profile name missing");
                }
                ColorSpace colorSpace = null;
                String iccProfileSrc = null;
                if (isPseudoProfile(iccProfileName)) {
                    if (CMYK_PSEUDO_PROFILE.equalsIgnoreCase(iccProfileName)) {
                        colorSpace = ColorSpaces.getDeviceCMYKColorSpace();
                    } else if (SEPARATION_PSEUDO_PROFILE.equalsIgnoreCase(iccProfileName)) {
                        colorSpace = new NamedColorSpace(args[5], sRGB);
                    } else {
                        assert false : "Incomplete implementation";
                    }
                } else {
                    /* Get and verify ICC profile source */
                    iccProfileSrc = args[4].trim();
                    if (iccProfileSrc == null || "".equals(iccProfileSrc)) {
                        throw new PropertyException("ICC profile source missing");
                    }
                    iccProfileSrc = unescapeString(iccProfileSrc);
                }
                /* ICC profile arguments */
                int componentStart = 4;
                if (colorSpace instanceof NamedColorSpace) {
                    componentStart++;
                }
                float[] iccComponents = new float[args.length - componentStart - 1];
                for (int ix = componentStart; ++ix < args.length;) {
                    iccComponents[ix - componentStart - 1] = Float.parseFloat(args[ix].trim());
                }
                if (colorSpace instanceof NamedColorSpace && iccComponents.length == 0) {
                    iccComponents = new float[] {1.0f}; //full tint if not specified
                }

                /* Ask FOP factory to get ColorSpace for the specified ICC profile source */
                if (foUserAgent != null && iccProfileSrc != null) {
                    colorSpace = foUserAgent.getFactory().getColorSpace(
                            foUserAgent.getBaseURL(), iccProfileSrc);
                }
                if (colorSpace != null) {
                    // ColorSpace available - create ColorExt (keeps track of replacement rgb
                    // values for possible later colorTOsRGBString call
                    ICCColor iccColor = new ICCColor(colorSpace, iccProfileName, iccProfileSrc,
                            iccComponents, 1.0f);
                    parsedColor = new ColorExt(red, green, blue, new Color[] {iccColor});
                } else {
                    // ICC profile could not be loaded - use rgb replacement values */
                    log.warn("Color profile '" + iccProfileSrc
                            + "' not found. Using sRGB replacement values.");
                    parsedColor = sRGB;
                }
            } catch (PropertyException pe) {
                //simply re-throw
                throw pe;
            } catch (Exception e) {
                //wrap in a PropertyException
                throw new PropertyException(e);
            }
        } else {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be fop-rgb-icc(r,g,b,NCNAME,src,....)");
        }
        return parsedColor;
    }

    /**
     * Parse a color specified using the fop-rgb-named-color() function.
     *
     * @param value the function call
     * @return a color if possible
     * @throws PropertyException if the format is wrong.
     */
    private static Color parseAsFopRgbNamedColor(FOUserAgent foUserAgent, String value)
            throws PropertyException {
        Color parsedColor;
        int poss = value.indexOf("(");
        int pose = value.indexOf(")");
        if (poss != -1 && pose != -1) {
            String[] args = value.substring(poss + 1, pose).split(",");

            try {
                if (args.length != 6) {
                    throw new PropertyException("rgb-named() function must have 6 arguments");
                }

                //Set up fallback sRGB value
                float red = Float.parseFloat(args[0].trim());
                float green = Float.parseFloat(args[1].trim());
                float blue = Float.parseFloat(args[2].trim());
                /* Verify rgb replacement arguments */
                if ((red < 0 || red > 1)
                        || (green < 0 || green > 1)
                        || (blue < 0 || blue > 1)) {
                    throw new PropertyException("Color values out of range. "
                            + "Fallback RGB arguments to fop-rgb-named-color() must be [0..1]");
                }
                Color sRGB = new ColorExt(red, green, blue, null);

                /* Get and verify ICC profile name */
                String iccProfileName = args[3].trim();
                if (iccProfileName == null || "".equals(iccProfileName)) {
                    throw new PropertyException("ICC profile name missing");
                }
                ICC_ColorSpace colorSpace = null;
                String iccProfileSrc = null;
                if (isPseudoProfile(iccProfileName)) {
                    throw new IllegalArgumentException(
                            "Pseudo-profiles are not allowed with fop-rgb-named-color()");
                } else {
                    /* Get and verify ICC profile source */
                    iccProfileSrc = args[4].trim();
                    if (iccProfileSrc == null || "".equals(iccProfileSrc)) {
                        throw new PropertyException("ICC profile source missing");
                    }
                    iccProfileSrc = unescapeString(iccProfileSrc);
                }

                // color name
                String colorName = unescapeString(args[5].trim());

                /* Ask FOP factory to get ColorSpace for the specified ICC profile source */
                if (foUserAgent != null && iccProfileSrc != null) {
                    colorSpace = (ICC_ColorSpace)foUserAgent.getFactory().getColorSpace(
                            foUserAgent.getBaseURL(), iccProfileSrc);
                }
                if (colorSpace != null) {
                    ICC_Profile profile = colorSpace.getProfile();
                    if (NamedColorProfileParser.isNamedColorProfile(profile)) {
                        NamedColorProfileParser parser = new NamedColorProfileParser();
                        NamedColorProfile ncp = parser.parseProfile(profile);
                        NamedColorSpace ncs = ncp.getNamedColor(colorName);
                        if (ncs != null) {
                            ICCColor iccColor = new ICCColor(ncs,
                                    iccProfileName, iccProfileSrc,
                                    new float[] {1.0f}, 1.0f);
                            parsedColor = new ColorExt(red, green, blue, new Color[] {iccColor});
                        } else {
                            log.warn("Color '" + colorName
                                    + "' does not exist in named color profile: " + iccProfileSrc);
                            parsedColor = sRGB;
                        }
                    } else {
                        log.warn("ICC profile is no named color profile: " + iccProfileSrc);
                        parsedColor = sRGB;
                    }
                } else {
                    // ICC profile could not be loaded - use rgb replacement values */
                    log.warn("Color profile '" + iccProfileSrc
                            + "' not found. Using sRGB replacement values.");
                    parsedColor = sRGB;
                }
            } catch (PropertyException pe) {
                //simply re-throw
                throw pe;
            } catch (Exception e) {
                //wrap in a PropertyException
                throw new PropertyException(e);
            }
        } else {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be fop-rgb-named-color(r,g,b,NCNAME,src,color-name)");
        }
        return parsedColor;
    }

    private static String unescapeString(String iccProfileSrc) {
        if (iccProfileSrc.startsWith("\"") || iccProfileSrc.startsWith("'")) {
            iccProfileSrc = iccProfileSrc.substring(1);
        }
        if (iccProfileSrc.endsWith("\"") || iccProfileSrc.endsWith("'")) {
            iccProfileSrc = iccProfileSrc.substring(0, iccProfileSrc.length() - 1);
        }
        return iccProfileSrc;
    }

    /**
     * Parse a color given with the cmyk() function.
     *
     * @param value
     *            the complete line
     * @return a color if possible
     * @throws PropertyException
     *             if the format is wrong.
     */
    private static Color parseAsCMYK(String value) throws PropertyException {
        Color parsedColor;
        int poss = value.indexOf("(");
        int pose = value.indexOf(")");
        if (poss != -1 && pose != -1) {
            value = value.substring(poss + 1, pose);
            String[] args = value.split(",");
            try {
                if (args.length != 4) {
                    throw new PropertyException(
                            "Invalid number of arguments: cmyk(" + value + ")");
                }
                float cyan = 0.0f, magenta = 0.0f, yellow = 0.0f, black = 0.0f;
                String str = args[0].trim();
                if (str.endsWith("%")) {
                  cyan  = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100.0f;
                } else {
                  cyan  = Float.parseFloat(str);
                }
                str = args[1].trim();
                if (str.endsWith("%")) {
                  magenta = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100.0f;
                } else {
                  magenta = Float.parseFloat(str);
                }
                str = args[2].trim();
                if (str.endsWith("%")) {
                  yellow = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100.0f;
                } else {
                  yellow = Float.parseFloat(str);
                }
                str = args[3].trim();
                if (str.endsWith("%")) {
                  black = Float.parseFloat(str.substring(0,
                            str.length() - 1)) / 100.0f;
                } else {
                  black = Float.parseFloat(str);
                }

                if ((cyan < 0.0 || cyan > 1.0)
                        || (magenta < 0.0 || magenta > 1.0)
                        || (yellow < 0.0 || yellow > 1.0)
                        || (black < 0.0 || black > 1.0)) {
                    throw new PropertyException("Color values out of range"
                            + "Arguments to cmyk() must be in the range [0%-100%] or [0.0-1.0]");
                }
                float[] comps = new float[] {cyan, magenta, yellow, black};
                parsedColor = DeviceCMYKColorSpace.createColorExt(comps);
            } catch (PropertyException pe) {
                throw pe;
            } catch (Exception e) {
                throw new PropertyException(e);
            }
        } else {
            throw new PropertyException("Unknown color format: " + value
                    + ". Must be cmyk(c,m,y,k)");
        }
        return parsedColor;
    }

    /**
     * Creates a re-parsable string representation of the given color.
     * <p>
     * First, the color will be converted into the sRGB colorspace. It will then
     * be printed as #rrggbb, or as #rrrggbbaa if an alpha value is present.
     *
     * @param color
     *            the color to represent.
     * @return a re-parsable string representadion.
     */
    public static String colorToString(Color color) {
        ColorSpace cs = color.getColorSpace();
        if (color instanceof ColorExt) {
            return toFunctionCall((ColorExt)color);
        } else if (cs != null && cs.getType() == ColorSpace.TYPE_CMYK) {
            StringBuffer sbuf = new StringBuffer(24);
            float[] cmyk = color.getColorComponents(null);
            sbuf.append("cmyk(" + cmyk[0] + "," + cmyk[1] + "," + cmyk[2] + "," +  cmyk[3] + ")");
            return sbuf.toString();
        } else {
            return toRGBFunctionCall(color);
        }
    }

    private static String toRGBFunctionCall(Color color) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('#');
        String s = Integer.toHexString(color.getRed());
        if (s.length() == 1) {
            sbuf.append('0');
        }
        sbuf.append(s);
        s = Integer.toHexString(color.getGreen());
        if (s.length() == 1) {
            sbuf.append('0');
        }
        sbuf.append(s);
        s = Integer.toHexString(color.getBlue());
        if (s.length() == 1) {
            sbuf.append('0');
        }
        sbuf.append(s);
        if (color.getAlpha() != 255) {
            s = Integer.toHexString(color.getAlpha());
            if (s.length() == 1) {
                sbuf.append('0');
            }
            sbuf.append(s);
        }
        return sbuf.toString();
    }

    /**
     * Create string representation of fop-rgb-icc function call to map this
     * ColorExt settings.
     * @param color the color to turn into a function call
     * @return the string representing the internal fop-rgb-icc() function call
     */
    public static String toFunctionCall(ColorExt color) {
        Color[] alt = color.getAlternateColors();
        if (alt.length == 0) {
            return toRGBFunctionCall(color);
        } else if (alt.length != 1) {
            throw new IllegalStateException(
                    "Cannot convert to function call: the number of alternate colors is not one.");
        }
        StringBuffer sb = new StringBuffer(40);
        sb.append("fop-rgb-icc(");
        float[] rgb = color.getColorComponents(null);
        assert rgb.length == 3;
        sb.append(rgb[0]).append(",");
        sb.append(rgb[1]).append(",");
        sb.append(rgb[2]).append(",");
        ICCColor icc = (ICCColor)alt[0];
        sb.append(icc.getColorProfileName()).append(",");
        if (icc.getColorProfileSource() != null) {
            sb.append("\"").append(icc.getColorProfileSource()).append("\"");
        }
        float[] colorComponents = icc.getColorComponents(null);
        for (int ix = 0; ix < colorComponents.length; ix++) {
            sb.append(",");
            sb.append(colorComponents[ix]);
        }
        sb.append(")");
        return sb.toString();
    }

    private static Color createColor(int r, int g, int b) {
        return new ColorExt(r, g, b, null);
    }

    /**
     * Initializes the colorMap with some predefined values.
     */
    private static void initializeColorMap() {
        colorMap = Collections.synchronizedMap(new java.util.HashMap());

        colorMap.put("aliceblue", createColor(240, 248, 255));
        colorMap.put("antiquewhite", createColor(250, 235, 215));
        colorMap.put("aqua", createColor(0, 255, 255));
        colorMap.put("aquamarine", createColor(127, 255, 212));
        colorMap.put("azure", createColor(240, 255, 255));
        colorMap.put("beige", createColor(245, 245, 220));
        colorMap.put("bisque", createColor(255, 228, 196));
        colorMap.put("black", createColor(0, 0, 0));
        colorMap.put("blanchedalmond", createColor(255, 235, 205));
        colorMap.put("blue", createColor(0, 0, 255));
        colorMap.put("blueviolet", createColor(138, 43, 226));
        colorMap.put("brown", createColor(165, 42, 42));
        colorMap.put("burlywood", createColor(222, 184, 135));
        colorMap.put("cadetblue", createColor(95, 158, 160));
        colorMap.put("chartreuse", createColor(127, 255, 0));
        colorMap.put("chocolate", createColor(210, 105, 30));
        colorMap.put("coral", createColor(255, 127, 80));
        colorMap.put("cornflowerblue", createColor(100, 149, 237));
        colorMap.put("cornsilk", createColor(255, 248, 220));
        colorMap.put("crimson", createColor(220, 20, 60));
        colorMap.put("cyan", createColor(0, 255, 255));
        colorMap.put("darkblue", createColor(0, 0, 139));
        colorMap.put("darkcyan", createColor(0, 139, 139));
        colorMap.put("darkgoldenrod", createColor(184, 134, 11));
        colorMap.put("darkgray", createColor(169, 169, 169));
        colorMap.put("darkgreen", createColor(0, 100, 0));
        colorMap.put("darkgrey", createColor(169, 169, 169));
        colorMap.put("darkkhaki", createColor(189, 183, 107));
        colorMap.put("darkmagenta", createColor(139, 0, 139));
        colorMap.put("darkolivegreen", createColor(85, 107, 47));
        colorMap.put("darkorange", createColor(255, 140, 0));
        colorMap.put("darkorchid", createColor(153, 50, 204));
        colorMap.put("darkred", createColor(139, 0, 0));
        colorMap.put("darksalmon", createColor(233, 150, 122));
        colorMap.put("darkseagreen", createColor(143, 188, 143));
        colorMap.put("darkslateblue", createColor(72, 61, 139));
        colorMap.put("darkslategray", createColor(47, 79, 79));
        colorMap.put("darkslategrey", createColor(47, 79, 79));
        colorMap.put("darkturquoise", createColor(0, 206, 209));
        colorMap.put("darkviolet", createColor(148, 0, 211));
        colorMap.put("deeppink", createColor(255, 20, 147));
        colorMap.put("deepskyblue", createColor(0, 191, 255));
        colorMap.put("dimgray", createColor(105, 105, 105));
        colorMap.put("dimgrey", createColor(105, 105, 105));
        colorMap.put("dodgerblue", createColor(30, 144, 255));
        colorMap.put("firebrick", createColor(178, 34, 34));
        colorMap.put("floralwhite", createColor(255, 250, 240));
        colorMap.put("forestgreen", createColor(34, 139, 34));
        colorMap.put("fuchsia", createColor(255, 0, 255));
        colorMap.put("gainsboro", createColor(220, 220, 220));
        colorMap.put("ghostwhite", createColor(248, 248, 255));
        colorMap.put("gold", createColor(255, 215, 0));
        colorMap.put("goldenrod", createColor(218, 165, 32));
        colorMap.put("gray", createColor(128, 128, 128));
        colorMap.put("green", createColor(0, 128, 0));
        colorMap.put("greenyellow", createColor(173, 255, 47));
        colorMap.put("grey", createColor(128, 128, 128));
        colorMap.put("honeydew", createColor(240, 255, 240));
        colorMap.put("hotpink", createColor(255, 105, 180));
        colorMap.put("indianred", createColor(205, 92, 92));
        colorMap.put("indigo", createColor(75, 0, 130));
        colorMap.put("ivory", createColor(255, 255, 240));
        colorMap.put("khaki", createColor(240, 230, 140));
        colorMap.put("lavender", createColor(230, 230, 250));
        colorMap.put("lavenderblush", createColor(255, 240, 245));
        colorMap.put("lawngreen", createColor(124, 252, 0));
        colorMap.put("lemonchiffon", createColor(255, 250, 205));
        colorMap.put("lightblue", createColor(173, 216, 230));
        colorMap.put("lightcoral", createColor(240, 128, 128));
        colorMap.put("lightcyan", createColor(224, 255, 255));
        colorMap.put("lightgoldenrodyellow", createColor(250, 250, 210));
        colorMap.put("lightgray", createColor(211, 211, 211));
        colorMap.put("lightgreen", createColor(144, 238, 144));
        colorMap.put("lightgrey", createColor(211, 211, 211));
        colorMap.put("lightpink", createColor(255, 182, 193));
        colorMap.put("lightsalmon", createColor(255, 160, 122));
        colorMap.put("lightseagreen", createColor(32, 178, 170));
        colorMap.put("lightskyblue", createColor(135, 206, 250));
        colorMap.put("lightslategray", createColor(119, 136, 153));
        colorMap.put("lightslategrey", createColor(119, 136, 153));
        colorMap.put("lightsteelblue", createColor(176, 196, 222));
        colorMap.put("lightyellow", createColor(255, 255, 224));
        colorMap.put("lime", createColor(0, 255, 0));
        colorMap.put("limegreen", createColor(50, 205, 50));
        colorMap.put("linen", createColor(250, 240, 230));
        colorMap.put("magenta", createColor(255, 0, 255));
        colorMap.put("maroon", createColor(128, 0, 0));
        colorMap.put("mediumaquamarine", createColor(102, 205, 170));
        colorMap.put("mediumblue", createColor(0, 0, 205));
        colorMap.put("mediumorchid", createColor(186, 85, 211));
        colorMap.put("mediumpurple", createColor(147, 112, 219));
        colorMap.put("mediumseagreen", createColor(60, 179, 113));
        colorMap.put("mediumslateblue", createColor(123, 104, 238));
        colorMap.put("mediumspringgreen", createColor(0, 250, 154));
        colorMap.put("mediumturquoise", createColor(72, 209, 204));
        colorMap.put("mediumvioletred", createColor(199, 21, 133));
        colorMap.put("midnightblue", createColor(25, 25, 112));
        colorMap.put("mintcream", createColor(245, 255, 250));
        colorMap.put("mistyrose", createColor(255, 228, 225));
        colorMap.put("moccasin", createColor(255, 228, 181));
        colorMap.put("navajowhite", createColor(255, 222, 173));
        colorMap.put("navy", createColor(0, 0, 128));
        colorMap.put("oldlace", createColor(253, 245, 230));
        colorMap.put("olive", createColor(128, 128, 0));
        colorMap.put("olivedrab", createColor(107, 142, 35));
        colorMap.put("orange", createColor(255, 165, 0));
        colorMap.put("orangered", createColor(255, 69, 0));
        colorMap.put("orchid", createColor(218, 112, 214));
        colorMap.put("palegoldenrod", createColor(238, 232, 170));
        colorMap.put("palegreen", createColor(152, 251, 152));
        colorMap.put("paleturquoise", createColor(175, 238, 238));
        colorMap.put("palevioletred", createColor(219, 112, 147));
        colorMap.put("papayawhip", createColor(255, 239, 213));
        colorMap.put("peachpuff", createColor(255, 218, 185));
        colorMap.put("peru", createColor(205, 133, 63));
        colorMap.put("pink", createColor(255, 192, 203));
        colorMap.put("plum ", createColor(221, 160, 221));
        colorMap.put("plum", createColor(221, 160, 221));
        colorMap.put("powderblue", createColor(176, 224, 230));
        colorMap.put("purple", createColor(128, 0, 128));
        colorMap.put("red", createColor(255, 0, 0));
        colorMap.put("rosybrown", createColor(188, 143, 143));
        colorMap.put("royalblue", createColor(65, 105, 225));
        colorMap.put("saddlebrown", createColor(139, 69, 19));
        colorMap.put("salmon", createColor(250, 128, 114));
        colorMap.put("sandybrown", createColor(244, 164, 96));
        colorMap.put("seagreen", createColor(46, 139, 87));
        colorMap.put("seashell", createColor(255, 245, 238));
        colorMap.put("sienna", createColor(160, 82, 45));
        colorMap.put("silver", createColor(192, 192, 192));
        colorMap.put("skyblue", createColor(135, 206, 235));
        colorMap.put("slateblue", createColor(106, 90, 205));
        colorMap.put("slategray", createColor(112, 128, 144));
        colorMap.put("slategrey", createColor(112, 128, 144));
        colorMap.put("snow", createColor(255, 250, 250));
        colorMap.put("springgreen", createColor(0, 255, 127));
        colorMap.put("steelblue", createColor(70, 130, 180));
        colorMap.put("tan", createColor(210, 180, 140));
        colorMap.put("teal", createColor(0, 128, 128));
        colorMap.put("thistle", createColor(216, 191, 216));
        colorMap.put("tomato", createColor(255, 99, 71));
        colorMap.put("turquoise", createColor(64, 224, 208));
        colorMap.put("violet", createColor(238, 130, 238));
        colorMap.put("wheat", createColor(245, 222, 179));
        colorMap.put("white", createColor(255, 255, 255));
        colorMap.put("whitesmoke", createColor(245, 245, 245));
        colorMap.put("yellow", createColor(255, 255, 0));
        colorMap.put("yellowgreen", createColor(154, 205, 50));
        colorMap.put("transparent", new ColorExt(0, 0, 0, 0, null));
    }

    /**
     * Lightens up a color for groove, ridge, inset and outset border effects.
     * @param col the color to lighten up
     * @param factor factor by which to lighten up (negative values darken the color)
     * @return the modified color
     */
    public static Color lightenColor(Color col, float factor) {
        return org.apache.xmlgraphics.java2d.color.ColorUtil.lightenColor(col, factor);
    }

    /**
     * Indicates whether the given color profile name is one of the pseudo-profiles supported
     * by FOP (ex. #CMYK).
     * @param colorProfileName the color profile name to check
     * @return true if the color profile name is of a built-in pseudo-profile
     */
    public static boolean isPseudoProfile(String colorProfileName) {
        return CMYK_PSEUDO_PROFILE.equalsIgnoreCase(colorProfileName)
                || SEPARATION_PSEUDO_PROFILE.equalsIgnoreCase(colorProfileName);
    }

    /**
     * Indicates whether the color is a gray value.
     * @param col the color
     * @return true if it is a gray value
     */
    public static boolean isGray(Color col) {
        return org.apache.xmlgraphics.java2d.color.ColorUtil.isGray(col);
    }

    /**
     * Creates an uncalibrary CMYK color with the given gray value.
     * @param black the gray component (0 - 1)
     * @return the CMYK color
     */
    public static Color toCMYKGrayColor(float black) {
        return org.apache.xmlgraphics.java2d.color.ColorUtil.toCMYKGrayColor(black);
    }
}
