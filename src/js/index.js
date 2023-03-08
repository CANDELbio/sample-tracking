import Select from 'react-select';
import EdiText from 'react-editext';
import { Modal, ModalHeader, ModalBody, ModalFooter, Tooltip} from 'reactstrap';
import { Checkbox } from 'react-input-checkbox';
import { DateInput, TimePicker } from "@blueprintjs/datetime";
import * as dayjs from 'dayjs';
var utc = require('dayjs/plugin/utc');
var timezone = require('dayjs/plugin/timezone');
dayjs.extend(utc);
dayjs.extend(timezone);
import firebase from 'firebase/app';
var firebaseui = require('firebaseui');
import StyledFirebaseAuth from 'react-firebaseui/StyledFirebaseAuth';


import blueprintDateTimeCss from "@blueprintjs/datetime/lib/css/blueprint-datetime.css";
import blueprintCss from "@blueprintjs/core/lib/css/blueprint.css";

window.ReactSelect = Select;
window.EdiText = EdiText;
window.Modal = Modal;
window.ModalHeader = ModalHeader;
window.ModalBody = ModalBody;
window.ModalFooter = ModalFooter;
window.Tooltip = Tooltip;
window.Checkbox = Checkbox;
window.DateInput = DateInput;
window.TimePicker = TimePicker;
window.dayjs = dayjs;
window.firebase = firebase;
window.firebaseui = firebaseui;
window.ReactFirebaseAuth = StyledFirebaseAuth;