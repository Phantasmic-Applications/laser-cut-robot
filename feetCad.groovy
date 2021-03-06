import com.neuronrobotics.bowlerstudio.creature.ICadGenerator;
import com.neuronrobotics.bowlerstudio.creature.CreatureLab;
import org.apache.commons.io.IOUtils;
import com.neuronrobotics.bowlerstudio.vitamins.*;
import eu.mihosoft.vrl.v3d.parametrics.*;
import javafx.scene.paint.Color;
import com.neuronrobotics.bowlerstudio.threed.BowlerStudio3dEngine;


class Feet implements ICadGenerator, IParameterChanged{
	//First we load teh default cad generator script 
	ICadGenerator defaultCadGen=(ICadGenerator) ScriptingEngine
	                    .gitScriptRun(
                                "https://github.com/madhephaestus/laser-cut-robot.git", // git location of the library
	                              "laserCutCad.groovy" , // file to load
	                              null
                        )
	LengthParameter thickness 		= new LengthParameter("Material Thickness",3.15,[10,1])
	LengthParameter headDiameter 		= new LengthParameter("Head Dimeter",100,[200,50])
	LengthParameter snoutLen 		= new LengthParameter("Snout Length",63,[200,50])
	LengthParameter jawHeight 		= new LengthParameter("Jaw Height",32,[200,10])
	LengthParameter leyeDiam 		= new LengthParameter("Left Eye Diameter",35,[headDiameter.getMM()/2,29])
	LengthParameter reyeDiam 		= new LengthParameter("Right Eye Diameter",35,[headDiameter.getMM()/2,29])
	LengthParameter eyeCenter 		= new LengthParameter("Eye Center Distance",headDiameter.getMM()/2,[headDiameter.getMM(),headDiameter.getMM()/2])
	StringParameter servoSizeParam 			= new StringParameter("hobbyServo Default","towerProMG91",Vitamins.listVitaminSizes("hobbyServo"))
	StringParameter boltSizeParam 			= new StringParameter("Bolt Size","M3",Vitamins.listVitaminSizes("capScrew"))

	HashMap<String, Object>  boltMeasurments = Vitamins.getConfiguration( "capScrew",boltSizeParam.getStrValue())
	HashMap<String, Object>  nutMeasurments = Vitamins.getConfiguration( "nut",boltSizeParam.getStrValue())
	//println boltMeasurments.toString() +" and "+nutMeasurments.toString()
	double boltDimeMeasurment = boltMeasurments.get("outerDiameter")
	double nutDimeMeasurment = nutMeasurments.get("width")
	double nutThickMeasurment = nutMeasurments.get("height")
	//private TransformNR offset =BowlerStudio3dEngine.getOffsetforvisualization().inverse();
	ArrayList<CSG> headParts =null
	@Override 
	public ArrayList<CSG> generateCad(DHParameterKinematics d, int linkIndex) {
		ArrayList<CSG> allCad=defaultCadGen.generateCad(d,linkIndex);
		ArrayList<DHLink> dhLinks=d.getChain().getLinks();
		DHLink dh = dhLinks.get(linkIndex)
		LinkConfiguration conf = d.getLinkConfiguration(linkIndex);
		HashMap<String, Object> shaftmap = Vitamins.getConfiguration(conf.getShaftType(),conf.getShaftSize())
		double hornOffset = 	shaftmap.get("hornThickness")	
		HashMap<String, Object> servoVitaminData = Vitamins.getConfiguration ("hobbyServo", "towerProMG91")

		//The link configuration
		// creating the servo
		CSG servoReference=   Vitamins.get(conf.getElectroMechanicalType(),conf.getElectroMechanicalSize())
		.transformed(new Transform().rotZ(90))
		//Creating the horn
		double servoTop = servoReference.getMaxZ()
		CSG horn = Vitamins.get(conf.getShaftType(),conf.getShaftSize())	
		
		
		//If you want you can add things here
		//allCad.add(myCSG);
		if(linkIndex ==dhLinks.size()-1){
			println "Found foot limb" 
			CSG foot =new Cylinder(20,20,thickness.getMM(),(int)30).toCSG() // a one line Cylinder

			/*CSG otherBit =new Cube(	40,// X dimention
								dh.getR(),// Y dimention
								thickness.getMM()//  Z dimention
								).toCSG()// this converts from the geometry to an object we can work with
								.toYMin()
								.toZMin()
								*/
			
			//moving the pary to the attachment pont
			//otherBit = defaultCadGen.moveDHValues(otherBit,dh)
			
			//defaultCadGen.add(allCad,otherBit,dh.getListener())
			defaultCadGen.add(allCad,foot,dh.getListener())
		}

		

			
			CSG bottomSpaced = new Cube (shaftmap.get("hornBaseDiameter")*1.5,dh.getR() , hornOffset * 2).toCSG().movez(-14).toYMin()
			//.movey(shaftmap.get("hornBaseDiameter")*2)
			
			
			
			CSG topKey = new Cube (shaftmap.get("hornBaseDiameter")*1.5,dh.getR() , hornOffset * 2).toCSG().toZMin().toYMin()
			
			
		

		double connectorCutOut = shaftmap.get("hornLength")

		CSG connectorBase = new Cube (shaftmap.get("hornBaseDiameter") * 2.5,10 , hornOffset * 4).toCSG().toXMin()
		CSG connectorRectangle = new Cube (hornOffset * 2, shaftmap.get("hornBaseDiameter")* 2.0, hornOffset * 5).toCSG()
		CSG testRectangle = new Cube (10,10,10).toCSG()
		CSG connectorHole = new Cylinder (5,5,40,(int)(30)).toCSG()
		CSG connector = connectorBase.union(connectorRectangle)
		connector.difference(connectorHole.movex(5))
		topKey.difference(connector.makeKeepaway(1.0))
		//CSG connector = connectorBase.union(testRectangle)
		connector = connector.rotz(90).movey(30)

		connector = defaultCadGen.moveDHValues(connector,dh)
		defaultCadGen.add(allCad,connector,dh.getListener())

		topKey = defaultCadGen.moveDHValues(topKey,dh)
			defaultCadGen.add(allCad, topKey, dh.getListener())

			bottomSpaced = defaultCadGen.moveDHValues(bottomSpaced, dh)
			bottomSpaced.movez(-14)
			bottomSpaced.difference(connector.makeKeepaway(2.85))
			defaultCadGen.add(allCad, bottomSpaced , dh.getListener())

		connector.setManufactuing({CSG arg0 ->
									return defaultCadGen.reverseDHValues(arg0.toZMin(),dh);
		});
		
		return allCad;
	}
	@Override 
	public ArrayList<CSG> generateBody(MobileBase b ) {
		ArrayList<CSG> allCad=defaultCadGen.generateBody(b);
		//If you want you can add things here
		//allCad.add(myCSG);
		return allCad;
	}
	/**
	 * This is a listener for a parameter changing
	 * @param name
	 * @param p
	 */
	 
	public void parameterChanged(String name, Parameter p){
		//new RuntimeException().printStackTrace(System.out);
		println "headParts was set to null from "+name
		new Exception().printStackTrace(System.out)
		headParts=null
	}
};

return new Feet()//Your code here