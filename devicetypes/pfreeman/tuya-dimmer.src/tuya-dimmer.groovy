/**
 *  Tuya-Tasmota Dimmer
 *
 *  Copyright 2018 Phillip Freeman
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
metadata {
	definition(name: "Tuya Dimmer", namespace: "PFreeman", author: "Phillip Freeman", runLocally: false, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false) {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Momentary"
		//capability "Polling"
		capability "Refresh"
        capability "Health Check"
	}

	// UI tile definitions
	tiles(scale: 2) {
    		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"momentary.push", icon:"st.switches.light.on", backgroundColor:"#00A0DC"
				attributeState "off", label:'${name}', action:"momentary.push", icon:"st.switches.light.off", backgroundColor:"#ffffff"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
		}
		main "switch"
	details(["switch", "refresh"])




/*    
    
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "momentary.push", icon: "st.switches.switch.on", backgroundColor: "#79b821"
				attributeState "off", label: '${name}', action: "momentary.push", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
		}
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}

		main "switch"
		details(["switch","levelSliderControl","refresh"])*/
	}

	preferences {
		input(name: "ipAddress", type: "string", title: "IP Address", description: "IP Address of Sonoff", displayDuringSetup: true, required: true)
		input(name: "port", type: "number", title: "Port", description: "Port", displayDuringSetup: true, required: true, defaultValue: 80)
		
		section("Sonoff Host") {
			
		}

		section("Authentication") {
			input(name: "username", type: "string", title: "Username", description: "Username", displayDuringSetup: false, required: false)
			input(name: "password", type: "password", title: "Password (sent cleartext)", description: "Caution: password is sent cleartext", displayDuringSetup: false, required: false)
		}
	}

	simulator {
        // status declarations specify messages that result from a person physically actuating the device
		// this is the message that the device will send to the Device Handler’s parse(message) method
		status "switch reports off": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:f1f55fbf-b2c8-470b-bc47-0bb4cb938f25, headers:SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL2pzb24NCkNvbnRlbnQtTGVuZ3RoOiAxNQ0KQ29ubmVjdGlvbjogY2xvc2U=, body:eyJQT1dFUiI6Ik9GRiJ9"
		status "switch reports on": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:dc77bc98-ddb3-409b-844a-0b8864082f39, headers:SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL2pzb24NCkNvbnRlbnQtTGVuZ3RoOiAxNA0KQ29ubmVjdGlvbjogY2xvc2U=, body:eyJQT1dFUiI6Ik9OIn0="
		status "poll status off": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:eee15c07-a24a-4fc6-8d8e-b1911ed6764f, headers:SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL2pzb24NCkNvbnRlbnQtTGVuZ3RoOiAxODkNCkNvbm5lY3Rpb246IGNsb3Nl, body:eyJTdGF0dXMiOnsiTW9kdWxlIjoxLCJGcmllbmRseU5hbWUiOiJTb25vZmYiLCJUb3BpYyI6ImRlZmF1bHQtdG9waWMiLCJCdXR0b25Ub3BpYyI6IjAiLCJQb3dlciI6MCwiUG93ZXJPblN0YXRlIjoxLCJMZWRTdGF0ZSI6MSwiU2F2ZURhdGEiOjEsIlNhdmVTdGF0ZSI6MCwiQnV0dG9uUmV0YWluIjowLCJQb3dlclJldGFpbiI6MH19"
		status "poll status on": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:b813e07c-4984-40c8-97bd-ba7603804ad0, headers:SFRUUC8xLjEgMjAwIE9LDQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL2pzb24NCkNvbnRlbnQtTGVuZ3RoOiAxODkNCkNvbm5lY3Rpb246IGNsb3Nl, body:eyJTdGF0dXMiOnsiTW9kdWxlIjoxLCJGcmllbmRseU5hbWUiOiJTb25vZmYiLCJUb3BpYyI6ImRlZmF1bHQtdG9waWMiLCJCdXR0b25Ub3BpYyI6IjAiLCJQb3dlciI6MSwiUG93ZXJPblN0YXRlIjoxLCJMZWRTdGF0ZSI6MSwiU2F2ZURhdGEiOjEsIlNhdmVTdGF0ZSI6MCwiQnV0dG9uUmV0YWluIjowLCJQb3dlclJldGFpbiI6MH19"
		status "legacy switch reports off": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:9f55a327-be15-43fb-91ce-c04a035a3217, headers:SFRUUC8xLjEgMjAwIE9LCkNvbnRlbnQtVHlwZTogdGV4dC9wbGFpbgpDb250ZW50LUxlbmd0aDogMzYKQ29ubmVjdGlvbjogY2xvc2U=, body:UkVTVUxUID0geyJQT1dFUiI6Ik9GRiJ9ClBPV0VSID0gT0ZG"
		status "legacy switch reports on": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:9f55a327-be15-43fb-91ce-c04a035a3217, headers:SFRUUC8xLjEgMjAwIE9LCkNvbnRlbnQtVHlwZTogdGV4dC9wbGFpbgpDb250ZW50LUxlbmd0aDogMzQKQ29ubmVjdGlvbjogY2xvc2U=, body:UkVTVUxUID0geyJQT1dFUiI6Ik9OIn0KUE9XRVIgPSBPTg=="
		status "legacy poll status off": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:9f55a327-be15-43fb-91ce-c04a035a3217, headers:SFRUUC8xLjEgMjAwIE9LCkNvbnRlbnQtVHlwZTogdGV4dC9wbGFpbgpDb250ZW50LUxlbmd0aDogMjE5CkNvbm5lY3Rpb246IGNsb3Nl, body:U1RBVFVTID0geyJTdGF0dXMiOnsiTW9kdWxlIjoxLCAiRnJpZW5kbHlOYW1lIjoiQmFzZW1lbnQgTGlnaHRzIiwgIlRvcGljIjoiYmFzZW1lbnQtbGlnaHRzIiwgIkJ1dHRvblRvcGljIjoiMCIsICJQb3dlciI6MCwgIlBvd2VyT25TdGF0ZSI6MywgIkxlZFN0YXRlIjoxLCAiU2F2ZURhdGEiOjEsICJTYXZlU3RhdGUiOjEsICJCdXR0b25SZXRhaW4iOjAsICJQb3dlclJldGFpbiI6MH19"
		status "legacy poll status on": "index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:9f55a327-be15-43fb-91ce-c04a035a3217, headers:SFRUUC8xLjEgMjAwIE9LCkNvbnRlbnQtVHlwZTogdGV4dC9wbGFpbgpDb250ZW50LUxlbmd0aDogMjE5CkNvbm5lY3Rpb246IGNsb3Nl, body:U1RBVFVTID0geyJTdGF0dXMiOnsiTW9kdWxlIjoxLCAiRnJpZW5kbHlOYW1lIjoiQmFzZW1lbnQgTGlnaHRzIiwgIlRvcGljIjoiYmFzZW1lbnQtbGlnaHRzIiwgIkJ1dHRvblRvcGljIjoiMCIsICJQb3dlciI6MSwgIlBvd2VyT25TdGF0ZSI6MywgIkxlZFN0YXRlIjoxLCAiU2F2ZURhdGEiOjEsICJTYXZlU3RhdGUiOjEsICJCdXR0b25SZXRhaW4iOjAsICJQb3dlclJldGFpbiI6MH19"

		// reply declarations specify responses that the physical device will send to the Device Handler
		// when it receives a certain message from the Hub
		// reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
    }
}

def installed() {
// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "physicalgraph.device.Protocol.LAN", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    //runEvery5Minutes("poll")
}

def updated() {
	log.debug "updated()"
	def commands = []

	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "physicalgraph.device.Protocol.LAN", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

	//log.debug "Scheduling poll for every 5 minute"
	unschedule("poll")
	//runEvery5Minutes("poll")
}

String testLegacyInput() {
	String prefix = 'index:15, mac:5CCF7FBD413C, ip:C0A80206, port:0050, requestId:9f55a327-be15-43fb-91ce-c04a035a3217'
	//String multiBody = '''STATUS = {"Status":{"Module":1, "FriendlyName":"Basement Lights", "Topic":"basement-lights", "ButtonTopic":"0", "Power":0, "PowerOnState":3, "LedState":1, "SaveData":1, "SaveState":1, "ButtonRetain":0, "PowerRetain":0}}'''
	String multiBody = '''RESULT = {"POWER":"ON"}
POWER = ON'''
	def contentLength = multiBody.length()
	String multiHeaders = """HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: $contentLength
Connection: close"""	
	String multilineString = prefix + ', headers:' + multiHeaders.bytes.encodeBase64() + ', body:' + multiBody.bytes.encodeBase64()
	return multilineString
}

def parse(String description) {
	log.debug "parse"
	def message = parseLanMessage(description)	//def message = parseLanMessage(testLegacyInput())
	//log.debug "description is $description"
	// parse result from current and legacy formats
	def resultJson = {}
	if (message?.json) {
		// current json data format
		resultJson = message.json
	}
	else {
		// legacy Content-Type: text/plain
		// with json embedded in body text
		def STATUS_PREFIX = "STATUS = "
		def RESULT_PREFIX = "RESULT = "
		if (message?.body?.startsWith(STATUS_PREFIX)) {
			resultJson = new groovy.json.JsonSlurper().parseText(message.body.substring(STATUS_PREFIX.length()))
		}
		else if (message?.body?.startsWith(RESULT_PREFIX)) {
			resultJson = new groovy.json.JsonSlurper().parseText(message.body.substring(RESULT_PREFIX.length()))
		}
	}
    
    log.debug(resultJson)

    if (resultJson?.POWER == "ON" || resultJson?.POWER1 == "ON" || resultJson?.StatusSTS?.POWER == "ON") {
        setSwitchState(true)
    }
    else if (resultJson?.POWER1 == "OFF" || resultJson?.POWER1 == "OFF" || resultJson?.StatusSTS?.POWER == "OFF") {
    	setSwitchState(false)
    }
    
    if (resultJson?.Dimmer) {
        setDimmerLevel(resultJson?.Dimmer)
    }

	resultJson?.StatusSTS.each {
    	if (it.key == "POWER" || it.key == "POWER1") {
        	setSwitchState(it.value == "ON" ? true : false)
        }
        else if (it.key == "Dimmer") {
        	setDimmerLevel(resultJson?.StatusSTS?.Dimmer)
        }
    }
}

def setSwitchState(Boolean isOn) {
	log.info "switch is " + (isOn ? "ON" : "OFF")
	sendEvent(name: "switch", value: isOn ? "on" : "off")
    //refresh() //don't call refresh here or it'll recurse indefinitly!
}

def setDimmerLevel(level) {
	log.info "dimmer is " + (level)
	sendEvent(name: "level", value: level)
    //refresh() //don't call refresh here or it'll recurse indefinitly!
}

def push() {
	log.debug "PUSH"
	sendCommand("Power", "Toggle")
    refresh()
}

def setLevel(value) {
	log.debug "LEVEL"
	sendCommand("Dimmer", value.toString())
    refresh()
}

def on() {
	log.debug "ON"
	sendCommand("Power", "On")
    refresh()
}

def off() {
	log.debug "OFF"
	sendCommand("Power", "Off")
    refresh()
}

def poll() {
	log.debug "POLL"
    refresh()
}

def refresh() {
	log.debug "REFRESH"
	sendCommand("Status", "11")
}

private def sendCommand(String command, String payload) {
	log.debug "sendCommand(${command}:${payload}) to device at $ipAddress:$port"

	if (!ipAddress || !port) {
		log.warn "aborting. ip address or port of device not set"
		return null;
	}

	def hosthex = convertIPtoHex(ipAddress)
	def porthex = convertPortToHex(port)
	device.deviceNetworkId = "$hosthex:$porthex"

	def path = "/cm"
	if (payload){
		path += "?cmnd=${command}%20${payload}"
	}
	else{
		path += "?cmnd=${command}"
	}

	if (username){
		path += "&user=${username}"
		if (password){
			path += "&password=${password}"
		}
	}
    
    log.debug("path: ${path}")

	sendHubCommand(new physicalgraph.device.HubAction(
		method: "GET",
		path: path,
		headers: [
			HOST: "${ipAddress}:${port}"
			]
		)
    )
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format('%04x', port.toInteger())
	return hexport
}


