import React from 'react';
import PropTypes from 'prop-types';
import { Button, Form, FormGroup, FormControl, ControlLabel, Col } from 'react-bootstrap';
import Dialog from './../../view/Dialog';
import FileTree from './../../view/FileTree';

/**
 * File Open Wizard Dialog
 * @extends React.Component
 */
class FileOpenDialog extends React.Component {

    /**
     * @inheritdoc
     */
    constructor(props) {
        super(props);
        this.state = {
            selectedNode: undefined,
            filePath: '',
            showDialog: true,
        };
        this.onFileOpen = this.onFileOpen.bind(this);
        this.onDialogHide = this.onDialogHide.bind(this);
    }

    /**
     * Called when user clicks open
     */
    onFileOpen() {
        this.setState({
            showDialog: false,
        });
    }

    /**
     * Called when user hides the dialog
     */
    onDialogHide() {
        this.setState({
            showDialog: false,
        });
    }

    /**
     * @inheritdoc
     */
    render() {
        return (
            <Dialog
                show={this.state.showDialog}
                title="Open File"
                actions={
                    <Button
                        bsStyle="primary"
                        onClick={this.onFileOpen}
                        disabled={this.state.filePath === ''}
                    >
                        Open
                    </Button>
                }
                closeAction
                onHide={this.onDialogHide}
            >
                <Form horizontal>
                    <FormGroup controlId="filePath">
                        <Col componentClass={ControlLabel} sm={2}>
                            File Path
                        </Col>
                        <Col sm={10}>
                            <FormControl
                                value={this.state.filePath}
                                onChange={(evt) => {
                                    this.setState({
                                        filePath: evt.target.value,
                                    });
                                }}
                                type="text"
                                placeholder="eg: /home/user/ballerina-services/routing.bal"
                            />
                        </Col>
                    </FormGroup>
                </Form>
                <FileTree
                    onSelect={
                        (node) => {
                            this.setState({
                                selectedNode: node,
                                filePath: node.id,
                            });
                        }
                    }
                />
            </Dialog>
        );
    }
}

FileOpenDialog.propTypes = {
    title: PropTypes.node,
};

export default FileOpenDialog;
