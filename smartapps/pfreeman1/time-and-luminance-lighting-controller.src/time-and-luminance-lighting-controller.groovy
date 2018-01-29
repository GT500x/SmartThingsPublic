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
    name: "Time and Luminance Lighting Controller",
    namespace: "pfreeman1",
    author: "Phillip Freeman",
    description: "Blah",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	
    section("Control these lights") {
		input "switches", "capability.switch", multiple: true
	}
    
    section("Select luminance sensor") {
		input "luxSensor", "capability.illuminanceMeasurement", required: true
	}

    section("...when luminance is ...") {
        input(name: "luxOperator", type: "enum", options: ["greaterThan" : "Greater Than", "lessThan" : "Less Than"], required: true)
    }
    
    section("...lux ...") {
        input "luxValue", type: "number", required: true
    }
    
    section("...do this...") {
        input(name: "turnLightsOn", type: "enum", options: [true : "Turn on the lights", false : "Turn off the lights"], required: true)
    }
    
    section("...set dimmable lights to this level...") {
		input "dimmerLevel", "number", title: "Dimmer level (1-100)",range: "1..100"
	}
    
    section("...shut off lights at this time...") {
		input "lightsOffTime", "time", required: false
    }
    
    section("Only turn on between what times?") {
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
    }
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", 
        	"Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
    section("Reset the app when entering these modes") {
        input(
            name		: "modes"
            ,type		: "mode"
            ,title		: "Set for specific mode(s)"
            ,multiple	: true
            ,required	: false
        )
        //input "resetFromTime", "time", title: "Mode-based reset only from this time:", required: false
        //input "resetToTime", "time", title: "...to this time:", required: false
    }
    section ("Don't adjust/set lights if this contact is open") {
        input(
            name		: "contact"
            ,title		: "If this contact is open, this app will wait."
            ,multiple	: false
            ,required	: false
            ,type		: "capability.contactSensor"
        )
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
    schedule("0 0 0 * * ?", resetApp)
    subscribe(location, "mode", modeChangeHandler)
    runEvery5Minutes(luxCheckHandler)
    
    if (lightsOffTime) {
    	schedule(lightsOffTime, lightsOffHandler)
    }
}

def modeChangeHandler(evt) {
	if (modes && evt.isStateChange() && state.hasRunToday) {
        if (modes.contains(location.mode)){
            log.trace "We just entered one of the configured modes to reset this app. I will now pretend that I haven't run yet today."
            resetApp()
            runIn(300, luxCheckHandler)
    	}
    }
}

def lightsOffHandler() {
	switches.off()
}

def resetApp() {
	state.hasRunToday = false
    //subscribe(luxSensor, "illuminance", luxChangeHandler)
    runEvery5Minutes(luxCheckHandler)
}

def luxCheckHandler (evt) {
	if (state.hasRunToday || !scheduleCheck(fromTime, toTime)) {
    	return
    }
    
	log.debug "Checking current lux reading."
	def crntLux = luxSensor.currentValue("illuminance").toInteger()
    
    if(luxOperator == "greaterThan" && crntLux < luxValue.toInteger()) {
    	log.debug "Configured to take action if current lux (${crntLux}) is greater than configured value (${luxValue}) - it is not."
    	return
    }
    
    if(luxOperator == "lessThan" && crntLux > luxValue.toInteger()) {
    	log.debug "Configured to take action if current lux (${crntLux}) is less than configured value (${luxValue}) - it is not."
    	return
    }
    
    if(contact) {
    	def contactState = contact.currentValue("contact")
        if (contactState) {
        	if (contactState == "open") {
            	log.debug "The configured contact, ${contact.displayName}, is open. Taking no action at this time."
                return
            }
            else {
            	log.debug "The configured contact, ${contact.displayName}, is closed."
            }
        }
    }
    
    log.trace "[${luxSensor.displayName}] All conditions have qualified - turning lights on/off"
    unschedule(luxCheckHandler)
    controlLights()
}

def scheduleCheck(start, end) {
    log.debug "scheduleCheck(): days: ${days}, fromTime: ${start}, toTime: ${end}"
	
    // Door is opened. Now check if today is one of the preset days-of-week
    def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    
    if (days) {
        def dayCheck = days.contains(day)
        if (dayCheck) {
        	if (start && end) {
                def between = timeOfDayIsBetween(start, end, new Date(), location.timeZone)
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
        if (start && end) {
            def between = timeOfDayIsBetween(start, end, new Date(), location.timeZone)
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
	state.hasRunToday = true
        
    if (!turnLightsOn) {
    	switches.off()
    	return
    }
    
    switches.each {
		if (it.currentState("switch").value != "on") {
			it.on()
		}
  
		if (it.supportedCommands.find{it.name == "setLevel"}) {
			it.setLevel(dimmerLevel.toInteger())
		}
	}
}