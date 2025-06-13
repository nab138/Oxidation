# Oxidation (states)

FRC Team 3044's state machine library for robot control.

https://github.com/user-attachments/assets/21bc96cb-4ddf-4471-ba24-15cd8c3310ff

(Above: the state machine, visualized in [shrinkwrap](https://github.com/nab138/shrinkwrap), automatically scoring coral)

## Design

The state machine is a control paradigm that sits on top of existing commands and subsystems. It allows for complex behaviors to be expressed in a way that is easy to understand and maintain.

It is made up of 2 main components:

- **States**: The individual states that the robot can be in. Each state has its own set of commands and triggers associated with it that run only while the robot is in that state. States can also have children.
- **Transitions**: The conditions that cause the robot to move from one state to another. These can be based on time, sensor input, or more complex logic.

The currently active state must be a "leaf" state, that is, one that has no children. If you attempt to transition directly to a parent state, it will recursively use entrance conditions and transitions to attempt to determine which leaf state to activate.
