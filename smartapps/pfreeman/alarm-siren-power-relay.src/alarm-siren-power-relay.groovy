definition(
    name: "Alarm Siren Power Relay",
    namespace: "pfreeman",
    author: "Phillip Freeman",
    description: "When an alarm siren is triggered, this app will turn something else on, and then turned it off when the siren is off, or after 5 minutes.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("When this siren is triggered"){
		input "alarm1", "capability.alarm"
	}
	section("Turn on a switch until siren is off, or after 5 minutes..."){
		input "switch1", "capability.switch"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(alarm1, "alarm.siren", alarmOnHandler)
    subscribe(alarm1, "alarm.both", alarmOnHandler)
    subscribe(alarm1, "alarm.off", alarmOffHandler)
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(alarm1, "alarm.siren", alarmOnHandler)
    subscribe(alarm1, "alarm.both", alarmOnHandler)
    subscribe(alarm1, "alarm.off", alarmOffHandler)
}

def alarmOnHandler(evt) {
	switch1.on()
	def fiveMinuteDelay = 60 * 5
	runIn(fiveMinuteDelay, turnOffSwitch)
}

def alarmOffHandler(evt) {
	switch1.off()
}

def turnOffSwitch() {
	switch1.off()
}