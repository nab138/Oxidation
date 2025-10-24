# Oxidation (states)

State machine library for intelligent FRC robot control.

https://github.com/user-attachments/assets/21bc96cb-4ddf-4471-ba24-15cd8c3310ff

(Above: the state machine, visualized in [shrinkwrap](https://github.com/nab138/shrinkwrap), automatically scoring coral)

## Design

The state machine is a control paradigm that sits on top of existing commands and subsystems. It allows for complex behaviors to be expressed in a way that is easy to understand and maintain.

It is made up of 2 main components:

- **States**: The individual states that the robot can be in. Each state has its own set of commands and triggers associated with it that run only while the robot is in that state. States can also have children.
- **Transitions**: The conditions that cause the robot to move from one state to another. These can be based on time, sensor input, or more complex logic.

The currently active state must be a "leaf" state, that is, one that has no children. If you attempt to transition directly to a parent state, it will recursively use entrance conditions and transitions to determine which leaf state to activate.

## Installation

Add the folowing to the repositories section of your `build.gradle`:

```gradle
maven {
  url = uri("https://maven.pkg.github.com/nab138/Oxidation")
  credentials {
      username = "3044-Packages-Bot"
      password = "\u0067\u0068\u0070\u005f\u0038\u0055\u0068\u0037\u0061\u004f\u0062\u0049\u004a\u0041\u005a\u0045\u0059\u0073\u0041\u0055\u0033\u0063\u0041\u0037\u004f\u0065\u0070\u0037\u0053\u0074\u0073\u0058\u0058\u0059\u0031\u004e\u006e\u0056\u0030\u004a"
  }
}
```

Then add this line to the dependencies section:

```gradle
implementation 'me.nabdev.oxidation:oxidation:0.0.0'
```

## Examples

- See [FRC Team 3044's 2025 Robot Code](https://github.com/FRCTeam3044/2025swervebase/tree/main/src/main/java/frc/robot/statemachine) for a complete example
