/**
 *  Auto Dimmer V2.1
 *
 *  Author: Mike Maxwell 
 	1.1 2014-12-21
    	--updated logging for more clarity
    1.2 2014-12-27
    	--complete rewrite
    1.3 2015-01-08
    	--corrected logic errors (3 lux settings map to 4 lux ranges and dimmer settings)
    1.4	2015-02-10
    	--added refresh  for dynamic page dimmer overrides
        --added free non commercial usage licensing
        --pretty typing cleanup
	1.5 2015-02-11
		--variable re-name fix
		--mapped 100 to 99% dimmer level
    1.6 2015-03-01
    	--fixed dimmer overrides that wouldn't stick sometimes
    1.7 2015-05-27
    	--Had to change mode input and methods, broke for some reason
 	1.8 2015-06-29
    	--updated description and renamed app
    1.9 2015-09-10
    	--changed log output for better clarity
	2.0 2015-09-29
		--complete front end re-write, dynamic pages galore
		--implemented dynamic adjustment option, with gradual adjustable ramp rate changes
    2.1	2015-09-30
    	--added dimmer specific level option (off)
 */
definition(
    name			: "autoDimmer",
    namespace		: "pfreeman1",
    author			: "Mike Maxwell & Phil Freeman", //Phil Freeman has made lots of changes to this.
    description		: "This add on smartApp automatically adjusts dimmer levels when dimmer(s) are turned on from physical switches or other smartApps.\n" +
					"Levels are set based on lux (illuminance) sensor readings and the dimmer levels that you specify." + 
					"This smartApp does not turn on dimmers directly, this allows you to retain all your existing on/off smartApps.\n"+
					"autoDimmer provides intelligent level management to your existing automations.",
    category		: "My Apps",
    iconUrl			: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url		: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url		: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@3x.png"
)

preferences {
	page(name: "main")
    page(name: "aboutMe", nextPage	: "main")
    page(name: "luxPage") //, nextPage	: "dimmersPage")
    page(name: "dimmersPage", nextPage	: "main")
    page(name: "overridePage")
    page(name: "dimmerOptions")
}

def installed() {
	atomicState.anyOptionsSet = false
   init()
}

def updated() {
	unsubscribe()
    unschedule()
    init()
}

def init(){
   	subscribe(dimmers, "switch.on", dimHandler)
   	subscribe(luxOmatic, "illuminance", luxHandler)
    try
    {
    def autoLuxers = settings.findAll{it.key.endsWith("_autoLux") && it?.value == true}
    //log.debug "autoLuxers: ${autoLuxers}"
    //log.debug "dimmers: ${dimmers}"
    if (autoLuxers){
        autoLuxers.each{ al ->
        	//log.debug "al: ${al}"
            //log.debug "al.key: ${al.key}, al.key.replace: ${al.key.replace("_autoLux","")}"
            
            //dimmers.each {log.debug "dimmer: ${it}, dimmer id: ${it.id}"}
        
        	def dimmer = dimmers.find{it.id == al.key.replace("_autoLux","")}
            
            //log.debug "dimmer: ${dimmer}"
            
            def prefVar = dimmer?.id + "_alwaysCheckLevel"
            log.debug "prefVar: ${prefVar}: ${this."${prefVar}"}"
            if(this."${prefVar}") {
            	log.debug "${dimmer.displayName} is set to always redim."
                subscribe(dimmer, "level", dimmerChangeHandler)
            }
        }           
	}
    luxHandler()
    }
    catch (e)
    {
    	log.debug "Exception: ${e}"
    }
}

def dimmerChangeHandler(evt) {
	if (luxOmatic.currentValue("illuminance").toInteger() == 0) {
		def dimmer = dimmers.find{it.id == evt.deviceId}
        
        if (dimmer.currentValue("switch") != "on") {
        	return
        }

        if(state["${dimmer.id}" + "_isDimming"] ?: false) {
        	log.debug "We're redimming, ignoring this event."
        }
        else {
        	log.trace "${dimmer.displayName} was manually adjusted. Starting to redim in 1 minute"
    		runIn(60,luxHandler)
        }
    }
}

def dimHandler(evt) {
	//get the dimmer that's been turned on
	def dimmer = dimmers.find{it.id == evt.deviceId}
	log.trace "${dimmer.displayName} was turned on..."
    setDimmer(dimmer,false)
}

def luxHandler(evt){
	//see if any dimmers are set to auto lux adjust
    def autoLuxers = settings.findAll{it.key.endsWith("_autoLux") && it?.value == true}
    if (autoLuxers){
        autoLuxers.each{ al ->
        	def dimmer = dimmers.find{it.id == al.key.replace("_autoLux","")}
            if (dimmer.currentValue("switch") == "on") {
            	if (evt){
            		log.trace "${dimmer.displayName} detected a change in illuminance... New value: ${evt.value}"
            	} else {
            		log.trace "${dimmer.displayName} detected a ramp request or dimmer check..."
            	}
            	setDimmer(dimmer,true)
             }
        }
     }
}

def isTimeOK(dimmer) {
    //log.debug "scheduleCheck(): days: ${days}, fromTime: ${fromTime}, toTime: ${toTime}"
    // Door is opened. Now check if today is one of the preset days-of-week
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    //def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    
    def prefVar = dimmer.id + "_fromTime"
    def fromTime = this."${prefVar}"
    prefVar = dimmer.id + "_toTime"
    def toTime = this."${prefVar}"

    if (fromTime && !toTime) {
		toTime = timeToday("23:59", location.timeZone)
    }
    
    log.debug "isTimeOK(): fromTime: ${fromTime}, toTime: ${toTime}"
    
    if (fromTime && toTime) {
        def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        if (between) {
            return true
        } 
    }
    else {
        return true
    }
    return false
}


def setDimmer(dimmer,isRamp){

	if (dimmer.currentValue("switch") != "on") {
    	log.debug "[${dimmer.displayName}] setDimmer: switch not on anymore! Ignoring this request"
    	return
    }
    
	def modeOK = modeIsOK()
	def timeOK = isTimeOK(dimmer)
    def contactOK = isContactOK(dimmer)
	if (modeOK && timeOK && contactOK ) {
    	def newLevel = 0
    	log.debug "[${dimmer.displayName}]: modeOK: True, timeOK: True"
    	//get its current dim level
    	def crntDimmerLevel = dimmer.currentValue("level").toInteger()
        log.debug "[${dimmer.displayName}]: Current dimmer level as reported by the device: ${crntDimmerLevel}"
    
    	//get currentLux reading
    	def crntLux = luxOmatic.currentValue("illuminance").toInteger()
       
        if (luxNoDimming && crntLux > luxNoDimming?.toInteger()) {
        	log.debug "[${dimmer.displayName}] Current lux is higher than the noDimming threshold - changeRequired: False "
            return
        }
        
        def prefVar = dimmer.id
    	def dimVar
    	if (crntLux < luxDark.toInteger()) {
    		//log.debug "mode:dark"
        	prefVar = prefVar + "_dark"
        	dimVar = dimDark
    	} else if (crntLux < luxDusk.toInteger()) {
    		//log.debug "mode:dusk"
            prefVar = prefVar + "_dusk"
            dimVar = dimDusk
  		} else if (crntLux < luxBright.toInteger()) {
    		//log.debug "mode:day"
            prefVar = prefVar + "_day"
            dimVar = dimDay
    	} else {
    		//log.debug "mode:bright"
    		prefVar = prefVar + "_bright"
        	dimVar = dimBright
    	}
        
        if (crntLux == 0 && (dim0Lux || this."${dimmer.id + "_0Lux"}")) {
            prefVar = dimmer.id + "_0Lux"
        	dimVar = dim0Lux
        }
        
    	def newDimmerLevel = (this."${prefVar}" ?: dimVar).toInteger()
        if (newDimmerLevel >= 100) newDimmerLevel = 99

        prefVar = dimmer.id + "_alwaysRamp"
        isRamp = (this."${prefVar}" ?: isRamp) //Check if the switch has the override configured that we will always ramp, even if the switch was just turned on.
        //log.debug "ramping override enabled is: ${(this."${prefVar}")}"
  
    	if ( Math.abs(newDimmerLevel - crntDimmerLevel) <= 4){
        	log.info "${dimmer.displayName}, changeRequired: False"
            state["${dimmer.id}_isDimming"] = false
            state["${dimmer.id}_prevDimmerLvl"] = -1
            state["${dimmer.id}_prevRampLvl"] = -1
        } else {
            log.info "[${dimmer.displayName}] changeRequired: True, isRamp: ${isRamp}"
            //if (!this."${prefVar}") log.debug "useDefaults: true"
    		//else log.debug "useDefaults: False"
            if (isRamp) {
            	if (newDimmerLevel == 0){
                	log.info "[${dimmer.displayName}] currentLevel:${crntDimmerLevel}%, requestedLevel:${newDimmerLevel}%, currentLux:${crntLux}"
	        		dimmer.off()
                } else {
            		def rampRate  = dimmer.id
                	rampRate = rampRate + "_ramp"
                	def rampInt = (this."${rampRate}" ?: 2).toInteger()
                	//log.debug "rampRate:${rampInt}"
                
            		def rampLevel 
                	if (crntDimmerLevel < newDimmerLevel){
                		rampLevel = crntDimmerLevel + rampInt
                	} else {
                		rampLevel = crntDimmerLevel - rampInt
                	}
        			
                    if ((state["${dimmer.id}_prevDimmerLvl"] ?: -1) == crntDimmerLevel) {
                        log.warn "[${dimmer.displayName}] Dimmer didn't change to new level last time... trying 5% higher/lower this time..."
                        
                        if (crntDimmerLevel < newDimmerLevel){
                        	rampLevel = state["${dimmer.id}_prevRampLvl"] + 5
                            if (rampLevel >= 100) {
                                rampLevel = 99
                                log.error "[${dimmer.displayName}] We're at max dimmer level but still having issues!"
                        	}
                        } else {
                        	rampLevel = state["${dimmer.id}_prevRampLvl"] - 5
                            if (rampLevel <= 0) {
                                rampLevel = 1
                                log.error "[${dimmer.displayName}] We're at min dimmer level but still having issues!"
                        	}
                        }
                    }
                    
                    log.info "[${dimmer.displayName}] currentLevel:${crntDimmerLevel}%, requestedLevel:${newDimmerLevel}%, rampLevel:${rampLevel}%, currentLux:${crntLux}"
                    state["${dimmer.id}_prevRampLvl"] = rampLevel
                    state["${dimmer.id}_prevDimmerLvl"] = crntDimmerLevel
                    dimmer.setLevel(rampLevel)
                    
                    //log.debug "Supported commands: ${dimmer.supportedCommands}"
                	
                    //prefVar = dimmer.id + "_alwaysCheckLevel"
                    //def alwaysReDim = this."${prefVar}" ?: false
                    //log.debug "alwaysReDim is: ${alwaysReDim}"
                   // if (Math.abs(rampLevel - newDimmerLevel) > 5){
                        	
                	//}
                    state["${dimmer.id}" + "_isDimming"] = true
                    runIn(60,luxHandler)
                }
            } else {
            	log.info "[${dimmer.displayName}] currentLevel:${crntDimmerLevel}%, requestedLevel:${newDimmerLevel}%, currentLux:${crntLux}"
            	if (newDimmerLevel == 0){
	        		dimmer.off()
                } else {
	        		dimmer.setLevel(newDimmerLevel)
                }
            }
        }
	} else {
        log.debug "[${dimmer.displayName}] modeOK: ${modeOK}, timeOK: ${timeOK}"
    }
}

/* whatever methods * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def modeIsOK() {
	def result = !modes || modes.contains(location.mode)
	return result
}

def isContactOK(dimmer) {
	def prefVar = dimmer.id + "_contact"
    def contact = this."${prefVar}"
    
    if (contact) {
    	log.debug "[${dimmer.displayName}] Checking contact: ${contact.displayName}"
        def contactState = contact.currentValue("contact")
        if (contactState == "open") {
        	log.debug "[${dimmer.displayName}] The contact is currently open. Dimming is paused."
            return false
        }
        else {
        	log.debug "[${dimmer.displayName}] The contact is currently closed, OK."
        }
    }
    else {
    	log.debug "[${dimmer.displayName}] isContactOK(): No contact associated with this dimmer."
    }
    return true
}

/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
    def isLuxComplete = luxPageComplete() == "complete"
    def isDimComplete = dimmersPageComplete() == "complete"
	def allComplete = isLuxComplete && isDimComplete  
    def toDo = null
    if (!isLuxComplete){
    	toDo = "luxPage"
    } else if (!isDimComplete){
    	toDo = "dimmersPage"
    }
    //log.info "main- allComplete: ${allComplete} lux:${isLuxComplete} dim:${isDimComplete}"

	return dynamicPage(
    	name		: "main"
        ,title		: "Main Page"
        ,nextPage	: toDo
        ,install	: allComplete
        ,uninstall	: true
        ){
            section(){
                     href(
                        name		: "ab"
                        ,title		: "About Me..." 
                        ,required	: false
                        ,page		: "aboutMe"
                        ,description: null
                     )
                     if (isLuxComplete){
                         href(
                            name		: "lp"
                            ,title		: "Illuminance settings..." 
                            ,required	: false
                            ,page		: "luxPage"
                            ,description: null
                            ,state		: luxPageComplete()
                        )
                    }
					if (isDimComplete){
                        href(
                            name		: "dp"
                            ,title		: "Dimmers and defaults..." 
                            ,required	: false
                            ,page		: "dimmersPage"
                            ,description: null
                            ,state		: dimmersPageComplete()
                        )
                    }
                    if (allComplete){
                        href(
                            name		: "op"
                            ,title		: "Specific dimmer settings..." 
                            ,required	: false
                            ,page		: "overridePage"
                            ,description: null
                            ,state		: anyOptionsSet()
                        )
					}
                    input(
                        name		: "modes"
                        ,type		: "mode"
                        ,title		: "Set for specific mode(s)"
                        ,multiple	: true
                        ,required	: false
                    )
            }
	}
}
def aboutMe(){
	return dynamicPage(name: "aboutMe"){
		section ("About Me"){
             paragraph 	"This add on smartApp automatically adjusts dimmer levels when dimmer(s) are turned on from physical switches or other smartApps.\n" +
						"Levels are set based on lux (illuminance) sensor readings and the dimmer levels that you specify." + 
						"This smartApp does not turn on dimmers directly, this allows you to retain all your existing on/off smartApps.\n"+
						"autoDimmer provides intelligent level management to your existing automations."
        }
    }
}
def luxPage(){
	//, nextPage	: "dimmersPage")
    def isDimComplete = dimmersPageComplete() == "complete"
    def toDo = null
    if (!isDimComplete){
    	toDo = "dimmersPage"
    } else if (!isDimComplete){
    	toDo = "main"
    }

    return dynamicPage(name: "luxPage",nextPage: toDo){
		section ("Illuminance settings"){
            input(
            	name		: "luxOmatic"
                ,title		: "Use this illuminance Sensor..."
                ,multiple	: false
                ,required	: true
                ,type		: "capability.illuminanceMeasurement"
            )
        }
        section("Select Lux levels"){    
            input(
            	name		: "luxDark"
                ,title		: "It's Dark below this level..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["10":"10 Lux","25":"25 Lux","50":"50 Lux","75":"75 Lux","100":"100 Lux"]
            )
            input(
            	name		: "luxDusk"
                ,title		: "Dusk/Dawn is between Dark and here..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["100":"100 Lux","125":"125 Lux","150":"150 Lux","175":"175 Lux","200":"200 Lux","300":"300 Lux","400":"400 Lux","500":"500 Lux","600":"600 Lux"]
            )
            input(
            	name		: "luxBright"
                ,title		: "Overcast is between Dusk/Dawn and here, above this level it's considered Sunny."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["500":"500 Lux","1000":"1000 Lux","2000":"2000 Lux","3000":"3000 Lux","4000":"4000 Lux","5000":"5000 Lux","6000":"6000 Lux","7000":"7000 Lux","8000":"8000 Lux","9000":"9000 Lux","10000":"10000 Lux"]
            )
            input(
            	name		: "luxNoDimming"
                ,title		: "Disable dimming if lux is above this level."
                ,multiple	: false
                ,required	: false
                ,type		: "enum"
                ,options	: ["4000":"4000 Lux","5000":"5000 Lux","6000":"6000 Lux","6000":"6000 Lux","7000":"7000 Lux","8000":"8000 Lux","9000":"9000 Lux","10000":"10000 Lux","15000":"15000 Lux","20000":"20000 Lux"]
            )
        }
	}
}
def dimmersPage(){
	return dynamicPage(name: "dimmersPage",title: "Dimmers and defaults"){
    	section ("Default dim levels for each brigtness range"){
            input(
                name		: "dim0Lux"
                ,title		: "When it's totally dark (0 lux)..."
                ,multiple	: false
                ,required	: false
                ,type		: "enum"
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
            )
            input(
                name		: "dimDark"
                ,title		: "When it's Dark out..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
            )
             input(
                name		: "dimDusk"
                ,title		: "For Dusk/Dawn use this..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum",
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
            )
            input(
                name		: "dimDay" 
                ,title		: "When it's Overcast..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
            )
			input(
                name		: "dimBright" 
                ,title		: "When it's Sunny..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
            )        
        }
		section ("Dimmers to manage"){
			input(
            	name		: "dimmers"
                ,multiple	: true
                ,required	: true
                ,type		: "capability.switchLevel"
            )
        }
	}
}
def overridePage(){
	atomicState.anyOptionsSet = false
	return dynamicPage(name: "overridePage"){
    	section("Specific dimmer settings"){
        	def sortedDimmers = dimmers.sort{it.displayName}
            sortedDimmers.each() { dimmer ->
                def safeName = dimmer.id
                //log.debug "safeName (dimmer.id): ${safeName}, dimmer.displayname: ${dimmer.displayName}"
                def name = dimmer.displayName
                href(
                    name		: safeName + "_pg"
                    ,title		: name
                    ,required	: false
                    ,page		: "dimmerOptions"
                    ,params		: [id:safeName,displayName:name]
                    ,description: null
                    ,state		: dimmerOptionsSelected(safeName)
                )
            }
		}
	}
}
def dimmerOptions(params){
	def safeName
    def displayName
    if (params.id) {
    	safeName = 	params.id
        displayName = params.displayName
    } else if (params.params) {
    	safeName = 	params.params.id
        displayName = params.params.displayName
    } 
    //log.info "safeName:${safeName} name:${displayName}"
    return dynamicPage(name: "dimmerOptions") {
            section("${displayName} Options") {
				input(
                	name					: safeName + "_autoLux"
                    ,title					: "Auto adjust levels during LUX changes"
                    ,required				: false
                    ,type					: "bool"
                )
                input(
                	name					: safeName + "_ramp"
                    ,title					: "Percent rate of change for Auto adjust (2% default)"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["1":"1%","2":"2%","5":"5%"]
                )
                input(
                	name					: safeName + "_alwaysRamp"
                    ,title					: "Always ramp even when light has just been turned on"
                    ,required				: false
                    ,type					: "bool"
                )
                input(
                	name					: safeName + "_alwaysCheckLevel"
                    ,title					: "Keep checking that the switch is at the target level even after it has reached it and when it's dark"
                    ,required				: false
                    ,type					: "bool"
                )
                input(
                	name					: safeName + "_fromTime"
                    ,title					: "Only autoDim starting at this time"
                    ,required				: false
                    ,type					: "time"
                )
                input(
                	name					: safeName + "_toTime"
                    ,title					: "Stop autoDimming after this time"
                    ,required				: false
                    ,type					: "time"
                )
                input(
                    name		: safeName + "_contact"
                    ,title		: "Select a contact which, when open, will pause auto dimming"
                    ,multiple	: false
                    ,required	: false
                    ,type		: "capability.contactSensor"
            	)
            }
            section("Set these to override the default settings."){
                input(
                    name					: safeName + "_0Lux"
                    ,title					: "0 Lux Level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["0":"Off","10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
                )
				input(
                    name					: safeName + "_dark"
                    ,title					: "Dark level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["0":"Off","10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
                )
                input(
                    name					: safeName + "_dusk" 
                    ,title					: "Dusk/Dawn level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["0":"Off","10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
                )
                input(
                    name					: safeName + "_day" 
                    ,title					: "Overcast level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["0":"Off","10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
                )
                input(
                    name					: safeName + "_bright" 
                    ,title					: "Bright level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: ["0":"Off","10":"10%","20":"20%","30":"30%","40":"40%","50":"50%","60":"60%","70":"70%","80":"80%","90":"90%","100":"100%"]
                )
			}
    }
}
/* href methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def dimmersPageComplete(){
	if (dimmers && dimDark && dimDusk && dimDay && dimBright){
    	return "complete"
    } else {
    	return null
    }
}
def dimmerOptionsSelected(safeName){
	def optionsList = ["${safeName}_autoLux","${safeName}_0Lux","${safeName}_dark","${safeName}_dusk","${safeName}_day","${safeName}_bright"]
    if (optionsList.find{this."${it}"}){
		atomicState.anyOptionsSet = true
		return "complete"
    } else {
    	return null
    }
}
def anyOptionsSet(){
    if (atomicState.anyOptionsSet) {
    	return "complete"
    } else {
    	return null
    }
}
def luxPageComplete(){
	if (luxOmatic && luxDark && luxDusk && luxBright){
    	return "complete"
    } else {
    	return null
    }
}