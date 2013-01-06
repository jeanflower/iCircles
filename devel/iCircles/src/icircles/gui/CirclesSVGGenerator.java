package icircles.gui;

import icircles.concreteDiagram.CircleContour;
import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.ConcreteSpider;
import icircles.concreteDiagram.ConcreteSpiderFoot;
import icircles.concreteDiagram.ConcreteSpiderLeg;
import icircles.concreteDiagram.ConcreteZone;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.svg.SVGDocument;

public class CirclesSVGGenerator {

    static enum zOrder {SHADING, CONTOUR, LABEL, SPIDER};

    private ConcreteDiagram diagram;

    /**
     *
     * @throws IllegalArgumentException
     */
    public CirclesSVGGenerator(ConcreteDiagram d) {
        if(null == d) {
            throw new IllegalArgumentException("ConcreteDiagram is null");
        }
        diagram = d;
    }

    /**
     * Draws a concreteDiagram as an SVG.
     *
     * This approach is wholly declarative.  It currently knows nothing about
     * the on screen rendering of the diagram.  To make decisions based on the
     * on screen rendering (such as better label placement) we will, in future,
     * have to build a GVT (from the Batik library) of the SVGDocument.
     *
     * @returns An SVGDocument DOM structure representing the SVG.
     */
    public SVGDocument toSVG() {
        // Get a DOMImplementation.
        DOMImplementation domImpl =
            SVGDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
        SVGDocument document = (SVGDocument) domImpl.createDocument(svgNS, "svg", null);

        // Get the root element (the 'svg' element).
        Element svgRoot = document.getDocumentElement();

        // Set the width and height attributes on the root 'svg' element.
        svgRoot.setAttributeNS(null, "width", Integer.toString(diagram.getSize()));
        svgRoot.setAttributeNS(null, "height", Integer.toString(diagram.getSize()));

        // Draw the shaded zones
        for(ConcreteZone z : diagram.getShadedZones()) {
            Element path = document.createElementNS(svgNS, "path");
            path.setAttributeNS(null, "d", toSVGPath(z.getShape(diagram.getBox())));
            path.setAttributeNS(null, "fill", "#cccccc"); // grey
            path.setAttributeNS(null, "z-index", Integer.toString(zOrder.SHADING.ordinal()));

            svgRoot.appendChild(path);
        }

        // TODO: Concrete* should return themselves as DocumentFragments
        for(CircleContour c : diagram.getCircles()) {
            // Draw the circle
            Element circle = document.createElementNS(svgNS, "circle");
            circle.setAttributeNS(null, "cx", Double.toString(c.get_cx()));
            circle.setAttributeNS(null, "cy", Double.toString(c.get_cy()));
            circle.setAttributeNS(null, "r", Double.toString(c.get_radius()));
            circle.setAttributeNS(null, "z-index", Integer.toString(zOrder.CONTOUR.ordinal()));
            // Not pretty, but it works.
            Color strokeColor = c.color();
            circle.setAttributeNS(null, "stroke",
                                  (null == strokeColor)
                                  ? "black"
                                  : "#" +  toHexString(c.color())
                                  );
            circle.setAttributeNS(null, "stroke-width", "2");
            circle.setAttributeNS(null, "fill", "none");
            svgRoot.appendChild(circle);

            // TODO: Put this text in a path around the circle
            // alternatively come up with some better label placement algorithm
            Element text = document.createElementNS(svgNS, "text");
            text.setAttributeNS(null, "x", Double.toString(c.get_cx()));
            text.setAttributeNS(null, "y", Double.toString(c.get_cy() + c.get_radius()));
            text.setAttributeNS(null,"text-anchor","middle");
            text.setAttributeNS(null, "fill",
                                  (null == strokeColor)
                                  ? "black"
                                  : "#" +  toHexString(c.color())
                                  );
            text.setAttributeNS(null, "z-index", Integer.toString(zOrder.LABEL.ordinal()));

            Text textNode =  document.createTextNode(c.ac.getLabel().getLabel());
            text.appendChild(textNode);
            svgRoot.appendChild(text);
        }

        for(ConcreteSpider cs : diagram.getSpiders()) {
            for(ConcreteSpiderFoot f : cs.feet) {
                // Draw the foot
                Element circle = document.createElementNS(svgNS, "circle");
                circle.setAttributeNS(null, "cx", Double.toString(f.getX()));
                circle.setAttributeNS(null, "cy", Double.toString(f.getY()));
                circle.setAttributeNS(null, "r", Double.toString(ConcreteSpiderFoot.FOOT_RADIUS));
                circle.setAttributeNS(null, "z-index", Integer.toString(zOrder.SPIDER.ordinal()));

                circle.setAttributeNS(null, "stroke", "black");
                circle.setAttributeNS(null, "stroke-width", "2");
                circle.setAttributeNS(null, "fill", "black");
                svgRoot.appendChild(circle);
            }

            for(ConcreteSpiderLeg l : cs.legs) {
                Element line = document.createElementNS(svgNS, "line");
                line.setAttributeNS(null, "x1", Double.toString(l.from.getX()));
                line.setAttributeNS(null, "y1", Double.toString(l.from.getY()));
                line.setAttributeNS(null, "x2", Double.toString(l.to.getX()));
                line.setAttributeNS(null, "y2", Double.toString(l.to.getY()));
                line.setAttributeNS(null, "z-index", Integer.toString(zOrder.SPIDER.ordinal()));

                line.setAttributeNS(null, "stroke", "black");
                line.setAttributeNS(null, "stroke-width", "2");
                line.setAttributeNS(null, "fill", "black");
                svgRoot.appendChild(line);
            }
        }
        return document;
    }

    /**
     * Converts a Java Color into a HTML color code.
     *
     * @param c The Java Color to convert.
     * @returns A HTML color code as a string prefixed with a '#' symbol.
     */
    private static String toHexString(Color c) {
        StringBuilder sb = new StringBuilder('#');

        if (c.getRed() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getRed()));

        if (c.getGreen() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getGreen()));

        if (c.getBlue() < 16) sb.append('0');
        sb.append(Integer.toHexString(c.getBlue()));

        return sb.toString();
    }

    /**
     * Converts an awt Area to a String representing an SVG path.
     *
     * @param a The Area to convert to an SVG path.
     * @returns An SVG specification of the passed in Area.
     */
    private static String toSVGPath(Area a) {
        StringBuilder sb = new StringBuilder();

        PathIterator it = a.getPathIterator(null);
        if (null == it) {
            return new String();
        }

        // PathIterator is not a normal Java Iterator
        while (!it.isDone()){
            double[] c = new double[6];

            switch(it.currentSegment(c)) {
            case PathIterator.SEG_MOVETO:
                sb.append(String.format("M%.2f,%.2f ", c[0], c[1]));
                break;
            case PathIterator.SEG_LINETO:
                sb.append(String.format("L%.2f,%.2f ", c[0], c[1]));
                break;
            case PathIterator.SEG_QUADTO:
                sb.append(String.format("Q%.2f,%.2f,%.2f,%.2f ",c[0], c[1], c[2], c[3]));
                break;
            case PathIterator.SEG_CUBICTO:
                sb.append(String.format("C%.2f,%.2f,%.2f,%.2f,%.2f,%.2f ", c[0], c[1], c[2], c[3], c[4], c[5]));
                break;
            case PathIterator.SEG_CLOSE:
                sb.append("Z");
                break;
            }

            // update
            it.next();
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        Document document = toSVG();

        // Use the old transformer method as we cannot be guaranteed that
        // the underlying JDK supports DOM level 3.
        try {
            Source source = new DOMSource(document.getDocumentElement());
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }
}