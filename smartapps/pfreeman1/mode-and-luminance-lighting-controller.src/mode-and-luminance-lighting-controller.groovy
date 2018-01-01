/**
 *  Time and Luminance Lighting Controller
 *
 *  Copyright 2017 Phillip Freeman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Mode and Luminance Lighting Controller",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "Blah",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Activate when entering these modes") {
        input(
            name		: "modes"
            ,type		: "mode"
            ,title		: "Set for specific mode(s)"
            ,multiple	: true
            ,required	: true
        )
    }
    section("AND only activate if leaving one of these modes") {
        input(
            name		: "leavingModes"
            ,type		: "mode"
            ,title		: "Set for specific mode(s)"
            ,multiple	: true
            ,required	: true
        )
    }
	
    section("Control these lights") {
		input "switches", "capability.switch", multiple: true
	}
    
    section("...do this...") {
        input(name: "turnLightsOn", type: "enum", options: [true : "Turn on the lights", false : "Turn off the lights"], required: true)
    }
    
    section("But only if this luminance sensor...") {
		input "luxSensor", "capability.illuminanceMeasurement", required: true
	}

    section("... is greater than or less than this lux value ...") {
        input(name: "luxOperator", type: "enum", options: ["greaterThan" : "Greater Than", "lessThan" : "Less Than"], required: true)
        input "luxValue", type: "number", required: true
    }

    section("Then, optionally, set dimmable lights to this level...") {
		input "dimmerLevel", "number", title: "Dimmer level (1-100)",range: "1..100", required: false
	}
    
    section("Only do this between what times?") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
    }
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", 
        	"Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
}

def initialize() {
	unsubscribe()
    unschedule()
    state.clear()
    //subscribe(luxSensor, "illuminance", luxCheckHandler)
    //schedule("0 0 0 * * ?", resetApp)
    //runEvery5Minutes(luxCheckHandler)
    subscribe(location, "mode", modeChangeHandler)
    state.prevMode = location.mode
}

def modeChangeHandler(evt) {
	if (evt.isStateChange()) {
        if (modes.contains(location.mode) && leavingModes.contains(state.prevMode) && scheduleOK() && luxOK() ){
            controlLights()
    	}
        state.prevMode = location.mode
    }
}


def luxOK () {
	log.debug "Checking current lux reading."
	def crntLux = luxSensor.currentValue("illuminance").toInteger()
    
    if(luxOperator == "greaterThan" && crntLux < luxValue.toInteger()) {
    	log.debug "Configured to take action if current lux (${crntLux}) is greater than configured value (${luxValue}) - it is not."
    	return false
    }
    
    if(luxOperator == "lessThan" && crntLux > luxValue.toInteger()) {
    	log.debug "Configured to take action if current lux (${crntLux}) is less than configured value (${luxValue}) - it is not."
    	return false
    }
    
    return true
}

def scheduleOK() {
    log.debug "scheduleOK(): days: ${days}, fromTime: ${fromTime}, toTime: ${toTime}"
	
    // Door is opened. Now check if today is one of the preset days-of-week
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    
    if (days) {
        def dayCheck = days.contains(day)
        if (dayCheck) {
        	if (fromTime && toTime) {
                def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
                if (between) {
                    return true
                } 
            }
            else {
            	return true
            }
        }
    }
    else {
        if (fromTime && toTime) {
            def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
            if (between) {
                return true
            } 
        }
        else {
            return true
        }
    }
    return false
}

def controlLights() {
    if (!turnLightsOn) {
    	switches.off()
    	return
    }
    
    switches.each {
		if (it.currentState("switch").value != "on") {
			it.on()
		}
  
		if (it.supportedCommands.find{it.name == "setLevel"} && dimmerLevel) {
			it.setLevel(dimmerLevel.toInteger())
		}
	}
}